#!/usr/bin/env python3
# Dumps the openfhe-python API for PythonParityTest, one line per overload:
#   kind|Class|name|minArgs|maxArgs|paramTypes   (paramTypes empty = only check name/arity)
# usage: extract_python_api.py <openfhe-python>/src/lib > src/test/resources/python-api.txt
import re
import sys

root = sys.argv[1]
files = ['bindings.cpp', 'binfhe_bindings.cpp', 'pke/serialization.cpp']
CCPARAMS = ['CCParamsBFVRNS', 'CCParamsBGVRNS', 'CCParamsCKKSRNS']


def strip_comments(s):
    s = re.sub(r'/\*.*?\*/', '', s, flags=re.S)
    return re.sub(r'//[^\n]*', '', s)


def call_body(src, start):
    """Text inside the parentheses that open at src[start-1]."""
    depth, i = 1, start
    while depth:
        c = src[i]
        if c == '"':
            i = src.index('"', i + 1)
        depth += (c == '(') - (c == ')')
        i += 1
    return src[start:i - 1], i


def statement_end(src, i):
    """Index of the ';' ending the statement that contains position i (bracket depth 0)."""
    depth = 0
    while True:
        c = src[i]
        if c == '"':
            i = src.index('"', i + 1)
        elif c in '({[':
            depth += 1
        elif c in ')}]':
            depth -= 1
        elif c == ';' and depth == 0:
            return i
        i += 1


def split_top(s):
    parts, depth, cur = [], 0, ''
    for c in s:
        if c in '(<[{': depth += 1
        if c in ')>]}': depth -= 1
        if c == ',' and depth == 0:
            parts.append(cur); cur = ''
        else:
            cur += c
    return parts + [cur]


JAVA_TYPES = [  # (regex on the C++ type, Java type); first match wins
    (r'std::function', 'DoubleUnaryOperator'),
    (r'py::function', 'LongBinaryOperator'),
    (r'vector<\s*std::vector<\s*Ciphertext', 'List'),
    (r'vector<\s*(Ciphertext|ReadOnlyCiphertext|PrivateKey|EvalKey|LWECiphertext|DCRTPoly)', 'List'),
    (r'vector<\s*(int64_t|uint64_t)', 'long[]'),
    (r'vector<\s*(uint32_t|int32_t|int|unsigned)', 'int[]'),
    (r'vector<\s*double', 'double[]'),
    (r'vector<\s*std::complex', 'Complex[]'),
    (r'std::map<\s*(uint32_t|unsigned)', 'EvalKeyMap'),
    (r'std::complex', 'Complex'),
    (r'LWECiphertext', 'LWECiphertext'),
    (r'LWEPrivateKey', 'LWEPrivateKey'),
    (r'Ciphertext', 'Ciphertext'),
    (r'\bLWEPlaintext(Modulus)?\b', 'long'),
    (r'Plaintext', 'Plaintext'),
    (r'PublicKey', 'PublicKey'),
    (r'PrivateKey', 'PrivateKey'),
    (r'EvalKey', 'EvalKey'),
    (r'KeyPair', 'KeyPair'),
    (r'CryptoContext', 'CryptoContext'),
    (r'BinFHEContext', 'BinFHEContext'),
    (r'SchSwchParams', 'SchSwchParams'),
    (r'ParmType', 'ParmType'),
    (r'\b(SecurityLevel|CKKSDataType|PlaintextEncodings|KeySwitchTechnique|MultipartyMode|'
     r'DecryptionNoiseMode|EncryptionTechnique|MultiplicationTechnique|CompressionLevel|'
     r'ProxyReEncryptionMode|SCHEME)\b', None),
    (r'SERJSON|SERBINARY', 'SerType'),
    (r'py::bytes', 'byte[]'),
    (r'std::string', 'String'),
    (r'\b(BINGATE|BINFHE_OUTPUT|BINFHE_PARAMSET|BINFHE_METHOD|KEYGEN_MODE|PKESchemeFeature|ScalingTechnique|'
     r'ExecutionMode|SecretKeyDist|Format)\b', None),
    (r'\b(int64_t|uint64_t|LWEPlaintext|LWEPlaintextModulus|size_t)\b', 'long'),
    (r'\b(uint32_t|int32_t|int|unsigned)\b', 'int'),
    (r'\bdouble\b', 'double'),
    (r'\bfloat\b', 'float'),
    (r'\bbool\b', 'boolean'),
]


def java_type(cpp):
    for rx, jt in JAVA_TYPES:
        m = re.search(rx, cpp)
        if m:
            return jt or m.group(1)
    raise ValueError('unmapped C++ type: ' + cpp)


def param_types(body, is_method, subst):
    """Java parameter types of a binding, or None when the binding text does not show them."""
    for k, v in subst.items():
        body = re.sub(r'\b' + k + r'\b', v, body)
    m = (re.search(r'static_cast<[^(]*\((?:\w+::)?\*\)\((.*?)\)\s*(const)?\s*>', body, re.S) or
         re.search(r'overload_cast<(.*?)>\(', body, re.S))
    if m:
        params, self_skip = m.group(1), 0
    else:
        m = re.search(r'\[[^\]]*\]\s*\(([^)]*)\)', body)
        if m:
            params, self_skip = m.group(1), (1 if is_method else 0)
        else:
            g = re.search(r'&GenCryptoContext<CryptoContext(\w+)>', body)
            if g:
                return ['CCParams' + g.group(1)]
            m = re.search(r'&(Serialize\w*Wrapper|Deserialize\w*Wrapper)<', body)
            if not m:
                return None
            depth, i = 1, m.end()
            while depth:
                depth += (body[i] == '<') - (body[i] == '>')
                i += 1
            fn, targs = m.group(1), split_top(body[m.end():i - 1])
            st = targs[-1]
            src_t = 'py::bytes' if 'Bytes' in fn else 'std::string'
            if fn.startswith('Serialize') and 'EvalMultKey' not in fn and 'EvalAutomorphismKey' not in fn:
                params = targs[0] + ',' + st
            elif fn.startswith('Serialize'):
                params = st + ', std::string'
            else:
                params = src_t + ',' + st
            self_skip = 0
    ps = [p for p in split_top(params) if p.strip()][self_skip:]
    return [java_type(p) for p in ps]


def arity(body, is_lambda_self):
    args = re.findall(r'py::arg\("\s*(\w+)\s*"\)(\s*=)?', body)
    if args:
        return len([a for a in args if not a[1]]), len(args)
    lam = re.search(r'\[[^\]]*\]\s*\(([^)]*)\)', body)
    if lam:
        params = [p for p in split_top(lam.group(1)) if p.strip()]
        n = len(params) - (1 if is_lambda_self else 0)
        return n, n
    return None, None


out = []
for f in files:
    src = strip_comments(open(f'{root}/{f}').read())
    spans = []
    for m in re.finditer(r'py::class_<([^;]*?)>\s*\(\s*m\s*,\s*("(\w+)"|name\.c_str\(\))\s*\)', src):
        name = m.group(3) or 'CCPARAMS'
        end = statement_end(src, m.end())
        spans.append((m.start(), end, name))
        out.append(('class', name, '', 0, 0, None))
    for m in re.finditer(r'auto\s+(\w+)\s*=\s*py::class_<[^;]*?>\s*\(\s*m\s*,\s*"(\w+)"\s*\)', src):
        var, name = m.group(1), m.group(2)
        func_end = src.index('\n}', m.end())
        spans.append((m.start(), func_end, name))
    templ = []  # (start, end, placeholder, instantiations)
    for m in re.finditer(r'(template\s*<typename (\w+)>\s*)?void\s+(bind_crypto_context_templates|bind_eval_\w+)\s*\([^)]*\)\s*\{', src):
        end = src.index('\n}', m.end())
        spans.append((m.start(), end, 'CryptoContext'))
        if m.group(2):
            insts = re.findall(m.group(3) + r'<([^;]*?)>\(cc_class\)', src)
            templ.append((m.start(), end, m.group(2), insts))

    def owner(pos):
        best = None
        for s, e, n in spans:
            if s <= pos <= e and (best is None or s >= best[0]):
                best = (s, e, n)
        return best[2] if best else None

    for m in re.finditer(r'(\bm|\bcls|\.)\s*\.?\s*def(_static)?\(\s*"(\w+)"', src):
        body, _ = call_body(src, m.end())
        is_module = m.group(1) == 'm'
        cls = None if is_module else owner(m.start())
        kind = 'function' if is_module else ('static' if m.group(2) else 'method')
        lo, hi = arity(body, not is_module and kind == 'method')
        substs = [{}]
        for ts, te, ph, insts in templ:
            if ts <= m.start() <= te:
                substs = [{ph: i} for i in insts]
        for subst in substs:
            types = param_types(body, kind == 'method', subst)
            if types is not None and lo is None:
                lo = hi = len(types)
            for c in (CCPARAMS if cls == 'CCPARAMS' else [cls]):
                out.append((kind, c or 'OpenFHE', m.group(3), lo, hi, types))
    for m in re.finditer(r'\.def\(\s*py::init<([^>]*)>\(\)\s*\)', src):
        cls = owner(m.start())
        n = len([p for p in m.group(1).split(',') if p.strip()])
        for c in (CCPARAMS if cls == 'CCPARAMS' else [cls]):
            out.append(('init', c, '<init>', n, n, None))
    for m in re.finditer(r'\.def\(\s*py::self\s*(==|!=)\s*py::self\s*\)', src):
        out.append(('method', owner(m.start()), {'==': '__eq__', '!=': '__ne__'}[m.group(1)], 1, 1, None))
    for m in re.finditer(r'\.def_readwrite\(\s*"(\w+)"', src):
        out.append(('field', owner(m.start()), m.group(1), 0, 0, None))
    for m in re.finditer(r'm\.attr\("(\w+)"\)', src):
        out.append(('constant', 'OpenFHE', m.group(1), 0, 0, None))

seen = set()
for kind, cls, name, lo, hi, types in out:
    if kind == 'class' and cls == 'CCPARAMS':
        continue
    t = '' if types is None else ','.join(types)
    line = f'{kind}|{cls}|{name}|{"" if lo is None else lo}|{"" if hi is None else hi}|{t}'
    if line not in seen:
        seen.add(line)
        print(line)

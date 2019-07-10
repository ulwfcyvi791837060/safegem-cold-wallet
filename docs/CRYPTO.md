## Hardware encryption chip for SAFEGEM These include:
DES/3DES

AES_128

SM3

SHA

TRNG

## Heavily optimized cryptography algorithms for SAFEGEM These include:
AES/Rijndael encryption/decryption

Big Number (256 bit) Arithmetics

BIP32 Hierarchical Deterministic Wallets

BIP39 Mnemonic code

ECDSA signing/verifying (supports secp256k1 and nist256p1 curves, uses RFC6979 for deterministic signatures)

ECDSA public key derivation

Base32 (RFC4648 and custom alphabets)

Base58 address representation

PBKDF2

SHA1

SHA2-256/SHA2-512

SHA3/Keccak


## Some parts of the library come from external sources:
- AES: https://github.com/BrianGladman/aes
- Base58: https://github.com/luke-jr/libbase58
- SHA1/SHA2: http://www.aarongifford.com/computers/sha.html
- SHA3: https://github.com/rhash/RHash
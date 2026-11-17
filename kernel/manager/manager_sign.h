#ifndef MANAGER_SIGN_H
#define MANAGER_SIGN_H

// AweYuki/AweYukiSU
#define EXPECTED_SIZE_AWEYUKISU 0x377
#define EXPECTED_HASH_AWEYUKISU "d3469712b6214462764a1d8d3e5cbe1d6819a0b629791b9f4101867821f1df64"

typedef struct {
    unsigned size;
    const char *sha256;
} apk_sign_key_t;

#endif /* MANAGER_SIGN_H */

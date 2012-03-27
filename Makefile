FILES := external/openssl/libs/armeabi/libcrypto.so \
	external/openssl/libs/armeabi/libssl.so \
	external/ipsec-tools/libs/armeabi/libipsec.so \
	external/ipsec-tools/libs/armeabi/libracoonlib.so \
	external/ipsec-tools/libs/armeabi/racoon \
	external/ipsec-tools/libs/armeabi/racoonctl \
	external/ipsec-tools/libs/armeabi/setkey

all: build install

clean: clean-rec

clean-rec:
	ndk-build -C external/openssl clean
	ndk-build -C external/ipsec-tools clean

build: build-openssl build-ipsec-tools

build-openssl:
	ndk-build -C external/openssl

build-ipsec-tools:
	OPENSSL_INC=$(PWD)/external/openssl/include OPENSSL_LIB=$(PWD)/external/openssl/libs/armeabi ndk-build -C external/ipsec-tools

install: build
	zip -j assets/ipsec-tools.zip $(FILES)

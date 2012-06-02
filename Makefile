INKSCAPE_FLAGS := --export-background-opacity=0


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
	test -e bin/ipsec-tools || mkdir bin/ipsec-tools
	cp $(FILES) bin/ipsec-tools
	mv bin/ipsec-tools/racoon bin/ipsec-tools/racoon.mikma
	zip -j assets/ipsec-tools.zip bin/ipsec-tools/*

play-icon:
	inkscape icon.svg --export-png=play-icon.png -w512 -h512 --export-background-opacity=0

icons:
	inkscape icon.svg --export-png=res/drawable-ldpi/icon.png -d 67 $(INKSCAPE_FLAGS)
	inkscape icon.svg --export-png=res/drawable-mdpi/icon.png -d 90 $(INKSCAPE_FLAGS)
	inkscape icon.svg --export-png=res/drawable-hdpi/icon.png -d 135 $(INKSCAPE_FLAGS)

	inkscape notification.svg --export-png=res/drawable-ldpi/notification.png -d 67 
	inkscape notification.svg --export-png=res/drawable-mdpi/notification.png -d 90 $(INKSCAPE_FLAGS)
	inkscape notification.svg --export-png=res/drawable-hdpi/notification.png -d 135 $(INKSCAPE_FLAGS)

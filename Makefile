INKSCAPE_FLAGS := --export-background-opacity=0

DIR_SSL := external/openssl/libs
DIR_IPSEC := external/ipsec-tools/libs

FILES_SSL := libcrypto.so \
	libssl.so

FILES_IPSEC := libipsec.so \
	libracoonlib.so \
	racoon \
	racoonctl \
	setkey

all: build install examples

examples:
	$(MAKE) -C example-config
	cp -a example-config/psk/policy.zip assets/example-psk.zip
	cp -a example-config/cert/policy.zip assets/example-cert.zip

clean: clean-rec

clean-rec:
	ndk-build -C external/openssl clean
	ndk-build -C external/ipsec-tools clean

build: build-openssl build-ipsec-tools

build-openssl: MAKE = ndk-build $(MAKEFLAGS)
build-openssl:
	$(MAKE) -C external/openssl

build-ipsec-tools: MAKE=ndk-build $(MAKEFLAGS)
build-ipsec-tools:
	OPENSSL_INC=$(PWD)/external/openssl/include OPENSSL_LIB=$(PWD)/external/openssl/libs $(MAKE) -C external/ipsec-tools

install: build
	test -e bin || mkdir bin
	test -e bin/ipsec-tools || mkdir bin/ipsec-tools
	test -e bin/armeabi || mkdir bin/armeabi
	test -e bin/x86 || mkdir bin/x86
	test -e assets/armeabi || mkdir assets/armeabi
	test -e assets/x86 || mkdir assets/x86
	for i in $(FILES_SSL); do cp $(DIR_SSL)/armeabi/$$i bin/armeabi; done
	for i in $(FILES_SSL); do cp $(DIR_SSL)/x86/$$i bin/x86; done
	for i in $(FILES_IPSEC); do cp $(DIR_IPSEC)/armeabi/$$i bin/armeabi; done
	for i in $(FILES_IPSEC); do cp $(DIR_IPSEC)/x86/$$i bin/x86; done
	mv bin/armeabi/racoon bin/armeabi/racoon.mikma
	mv bin/x86/racoon bin/x86/racoon.mikma
	zip -j assets/armeabi/ipsec-tools.zip bin/armeabi/*
	zip -j assets/x86/ipsec-tools.zip bin/x86/*

play-icon:
	inkscape icon.svg --export-png=play/icon.png -w512 -h512 --export-background-opacity=0

icons:
	inkscape icon.svg --export-png=res/drawable-ldpi/icon.png -d 67 $(INKSCAPE_FLAGS)
	inkscape icon.svg --export-png=res/drawable-mdpi/icon.png -d 90 $(INKSCAPE_FLAGS)
	inkscape icon.svg --export-png=res/drawable-hdpi/icon.png -d 135 $(INKSCAPE_FLAGS)

	inkscape notification.svg --export-png=res/drawable-ldpi/notification.png -d 67 
	inkscape notification.svg --export-png=res/drawable-mdpi/notification.png -d 90 $(INKSCAPE_FLAGS)
	inkscape notification.svg --export-png=res/drawable-hdpi/notification.png -d 135 $(INKSCAPE_FLAGS)

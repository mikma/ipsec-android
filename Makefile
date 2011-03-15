PREFIX := /data/local

all: build install

clean: clean-rec
#	$(RM) -r openssl/obj
#	$(RM) -r openssl/libs
#	$(RM) -r openssl/crypto/libs
#	$(RM) -r ipsec-tools/obj
#	$(RM) -r ipsec-tools/libs

clean-rec:
	ndk-build -C openssl clean
	ndk-build -C ipsec-tools

build:
	touch --date="2011-03-06" openssl/Android.mk && ndk-build -C openssl
	touch --date="2011-03-06" ipsec-tools/Android.mk && touch --date="2011-03-06" ipsec-tools/src/racoon/Android.mk && OPENSSL_INC=$(PWD)/openssl/include OPENSSL_LIB=$(PWD)/openssl/libs/armeabi ndk-build -C ipsec-tools

install:
	adb push openssl/libs/armeabi/libcrypto.so $(PREFIX)
	adb push openssl/libs/armeabi/libssl.so $(PREFIX)
	adb push openssl/libs/armeabi/openssl $(PREFIX)
	adb push ipsec-tools/libs/armeabi/libipsec.so $(PREFIX)
	adb push ipsec-tools/libs/armeabi/racoon $(PREFIX)
	adb push ipsec-tools/libs/armeabi/setkey $(PREFIX)

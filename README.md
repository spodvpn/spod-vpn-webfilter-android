# Spod VPN & Web Filter mobile client for Android

Spod’s virtual network is the combination of a VPN and a firewall to bring your iOS device the security, privacy and anonymity it deserves!

## VPN - Virtual Private Network service

Our VPN service is focused on develivering more security, privacy and anonymity for mobile users.

* Security and Privacy: Everything between your device and our servers is encrypted using AES-256 to keep third parties from snooping into your traffic including which apps/websites you use and its data (username, passwords, etc).
* Anonymity: We created a simple way to anonymize everything that goes through our network: There’s no registration!
* Multi-Region: Choose between servers in the US or Brazil to keep a low latency or simply to connect from another country.
* Alerts: The Alerts tab inside the App will show you everything that was blocked by the Web Filter.
* Custom lists: Now you can unblock a specific tracker/threat and also configure a custom hostname to be blocked!
* Multi-Platform: Single subscription for both iOS and macOS.


## Web Filter firewall

One of the greatest benefits of our virtual network is the Web Filter feature which blocks unwanted and malicious content from your device. This content is divided into 2 categories:

- Trackers: These trackers are everywhere on the internet and they monitor you to show relevant ads and gather information.
- Threats: The threats are divided between resources spreading malicious content and phishing websites.

## Download the app
You can download Spod VPN & Web Filter for [Android](http://play.google.com/store/apps/details?id=br.com.spod.spodvpnwebfilter), [iOS](https://itunes.apple.com/app/id1441670465) and [macOS](https://apps.apple.com/us/app/spod-vpn-filtro-web/id1466110599).

## Building the project for Android:

1. [Requirement] Android development environment such as Android Studio.
2. [Requirement] Android NDK for building strongSwan's JNI libraries: https://developer.android.com/ndk
3. Clone strongSwan's official [repository](https://github.com/strongswan/strongswan) into [strongswan-src/strongswan](strongswan-src/) folder:
```shell
$ git clone https://github.com/strongswan/strongswan/ strongswan-src/strongswan
```
4. Clone BoringSSL from strongSwan into [strongswan/src/main/jni/openssl](strongswan/src/main/jni/) folder: 
```shell
$ git clone git://git.strongswan.org/android-ndk-boringssl.git -b ndk-static strongswan/src/main/jni/openssl
```
> Create openssl folder symlink:
```shell
$ ln -s ../../../../../../../../../strongswan/src/main/jni/openssl strongswan-src/strongswan/src/frontends/android/app/src/main/jni/openssl
```
5. Configure and build strongSwan from [strongswan-src/strongswan](strongswan-src/) folder: 
```shell
$ cd strongswan-src/strongswan
$ ./autogen.sh && ./configure && make dist
```
6. Done, you can load the Spod VPN & Web Filter project into Android Studio and build it from there.




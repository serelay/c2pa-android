# C2PA Android

## ⚠️ This project is intended to be forked and will not be maintained here. ⚠️


Bootstrap for client-side asset creation for the Coalition for Content Provenance and 
Authenticity ([C2PA](https://c2pa.org)) standard. 

**This was created targeting C2PA Draft Specification v0.7 (C2PA PUBLIC DRAFT, 2021-08-31)**

The approach may also be applied to C2PA's predecessor CAI with minor client-side adjustments.

Privacy-respecting flow relies upon a level of trust between the device. e.g. via SafetyNet or Firebase AppCheck. This version implicitly trusts the hashes provided by the client device. However, due to the cryptographic protections in place within C2PA specification the underlying asset is altered after the server request, then the C2PA validations will highlight this.

Alternatively, where the trust in the client environment is lower, the full image can be provided to the server toolkit, and appropriate hashes will be compiled. But rather than respond with a full-image download the server can respond with data portions and instructions to the client on where to insert them in the original asset.

It is good practice to retain control of signing keys off-device in case of abuse. This library intends to provide some tools for this.

## Usage

This is written as an individual module, with a reference AAR implementation in order to support a faster copy & paste into an existing project.

A reference implementation can be found in the `demo-app` module.

With minimal adjustments this should be compatible with regular Java/Kotlin, outside of the Android ecosystem.

**Example flow**
* User selects image to create a C2PA assertion.
* Device calculates some C2PA information for a server module to use.

**Either**

**Scenario A (Server-side thumbnail generation)**
* Device requests signed C2PA information to write, providing original image.
* Server produces full C2PA information, with embedded thumbnail data.
* Device receives full C2PA information.
* Device processes information and writes content to a copy of the original file.

**Or**

**Scenario B (Client-side thumbnail generation)**
* Device creates thumbnail for C2PA from original asset.
* Device provides server with hash and thumbnail information points for C2PA generation.
* Server constructs C2PA information with placeholder data as the thumbnail portion.
* Server signs this, trusting the client-provided thumbnail hash and length.
* Device receives data with JUMBF thumbnail placeholder data omitted.
* Device augments JUMBF segments with real thumbnail data.
* Device writes XMP information and thumbnail-augmented JUMBF segments to a copy of the original file.

## Documentation generation
Run `./gradlew dokkaHtmlMultiModule` to generate docs locally.
The docs will appear in `docs/api` for you to browse.

## Project Structure

### android-libjpeg-turbo
This module is built to allow for reproducible thumbnails across different architectures and OEMs.
This holds one method to accomplish this within `jpegturbowrapper.c`. This
could be adapted to suit different needs.

### demo-app
A reference implementation with trivial usage examples.

### lib
The module with the technical implementation - this would be the ideal copy & paste candidate for adding to a separate project.

## Thumbnails
During development it was discovered that Thumbnails for the same source JPEG could differ depending
on the device manufacturer, operating system version and model.
This is in part due to JPEG being a lossy format, but also platform differences.

Alternatively a custom thumbnail can be created via Android APIs such as 
[ThumbnailUtils](https://developer.android.com/reference/android/media/ThumbnailUtils) or 
via Bitmap matrix transforms. But this also has differences depending on device.

If the same device is creating the image and/or hashes, or thumbnails are stored differently 
it is likely that this requirement can be avoided.

## Native binaries

`android-libjpeg-turbo/intermediates/build/library_and_local_jars_jni` needs to be copied directly 
into the lib's jniLib folder as folders, containing the respective architecture `.so` files in order
to be built into the resulting Android library AAR file. The `lib` gradle file also allows for
in-project development.

# Related projects

- [C2PA Android](https://github.com/serelay/c2pa-android)
- [C2PA iOS](https://github.com/serelay/c2pa-ios)
- [C2PA Node](https://github.com/serelay/c2pa-node)
- [C2PA Web](https://github.com/serelay/c2pa-web)

# Thanks
Thanks to [@IanField90](https://github.com/IanField90), [@iblamefish](https://github.com/iblamefish), [@lesh1k](https://github.com/lesh1k) for making this possible.
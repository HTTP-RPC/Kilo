FRAMEWORK=HTTPRPC

BUILD=build
FRAMEWORK_PATH=$FRAMEWORK.framework

# iOS
rm -Rf $FRAMEWORK-iOS/$BUILD
rm -f $FRAMEWORK-iOS.framework.tar.gz

xcodebuild archive -project $FRAMEWORK-iOS/$FRAMEWORK-iOS.xcodeproj -scheme $FRAMEWORK -sdk iphoneos SYMROOT=$BUILD
xcodebuild build -project $FRAMEWORK-iOS/$FRAMEWORK-iOS.xcodeproj -target $FRAMEWORK -sdk iphonesimulator SYMROOT=$BUILD

cp -RL $FRAMEWORK-iOS/$BUILD/Release-iphoneos $FRAMEWORK-iOS/$BUILD/Release-universal

lipo -create $FRAMEWORK-iOS/$BUILD/Release-iphoneos/$FRAMEWORK_PATH/$FRAMEWORK $FRAMEWORK-iOS/$BUILD/Release-iphonesimulator/$FRAMEWORK_PATH/$FRAMEWORK -output $FRAMEWORK-iOS/$BUILD/Release-universal/$FRAMEWORK_PATH/$FRAMEWORK

tar -czv -C $FRAMEWORK-iOS/$BUILD/Release-universal -f $FRAMEWORK-iOS.tar.gz $FRAMEWORK_PATH $FRAMEWORK_PATH.dSYM

# tvOS
rm -Rf $FRAMEWORK-tvOS/$BUILD
rm -f $FRAMEWORK-tvOS.framework.tar.gz

xcodebuild archive -project $FRAMEWORK-tvOS/$FRAMEWORK-tvOS.xcodeproj -scheme $FRAMEWORK -sdk appletvos SYMROOT=$BUILD
xcodebuild build -project $FRAMEWORK-tvOS/$FRAMEWORK-tvOS.xcodeproj -target $FRAMEWORK -sdk appletvsimulator SYMROOT=$BUILD

cp -RL $FRAMEWORK-tvOS/$BUILD/Release-appletvos $FRAMEWORK-tvOS/$BUILD/Release-universal

lipo -create $FRAMEWORK-tvOS/$BUILD/Release-appletvos/$FRAMEWORK_PATH/$FRAMEWORK $FRAMEWORK-tvOS/$BUILD/Release-appletvsimulator/$FRAMEWORK_PATH/$FRAMEWORK -output $FRAMEWORK-tvOS/$BUILD/Release-universal/$FRAMEWORK_PATH/$FRAMEWORK

tar -czv -C $FRAMEWORK-tvOS/$BUILD/Release-universal -f $FRAMEWORK-tvOS.tar.gz $FRAMEWORK_PATH $FRAMEWORK_PATH.dSYM

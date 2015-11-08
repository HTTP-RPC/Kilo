export FRAMEWORK=HTTPRPC

rm -Rf build
rm $FRAMEWORK.framework.tar.gz

xcodebuild -project $FRAMEWORK.xcodeproj -sdk iphonesimulator -target $FRAMEWORK
xcodebuild -project $FRAMEWORK.xcodeproj -sdk iphoneos -target $FRAMEWORK 

lipo build/Release-iphonesimulator/$FRAMEWORK.framework/$FRAMEWORK build/Release-iphoneos/$FRAMEWORK.framework/$FRAMEWORK -create -output $FRAMEWORK.lipo

mv $FRAMEWORK.lipo build/Release-iphoneos/$FRAMEWORK.framework/$FRAMEWORK

tar -czv -C build/Release-iphoneos -f $FRAMEWORK.framework.tar.gz $FRAMEWORK.framework

Pod::Spec.new do |s|
  s.name            = 'HTTPRPC'
  s.version         = '4.2.3'
  s.license         = 'Apache License, Version 2.0'
  s.homepage        = 'https://github.com/gk-brown/HTTP-RPC'
  s.author          = 'Greg Brown'
  s.summary         = 'Lightweight multi-platform REST'
  s.source          = { :git => "https://github.com/gk-brown/HTTP-RPC.git", :tag => s.version.to_s }

  s.ios.deployment_target   = '9.0'
  s.ios.source_files        = 'Client/Objective-C/HTTPRPC-iOS/HTTPRPC/*.{h,m}'
  s.tvos.deployment_target  = '10.0'
  s.tvos.source_files       = 'Client/Objective-C/HTTPRPC-iOS/HTTPRPC/*.{h,m}'
end

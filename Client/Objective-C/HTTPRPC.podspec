Pod::Spec.new do |s|
  s.name             = 'HTTPRPC'
  s.version          = '3.3'
  s.summary          = 'Cross-platform RPC over HTTP'
  s.description      = <<-DESC
    HTTP-RPC is a mechanism for executing remote procedure calls via HTTP. It combines the
    flexibility of SOAP with the simplicity of REST, allowing callers to invoke arbitrary
    operations on a remote endpoint using human-readable URLs and JSON rather than complex
    XML messages and descriptors.
    DESC
  s.homepage         = 'https://github.com/gk-brown/HTTP-RPC'
  s.license          = 'Apache License, Version 2.0'
  s.author           = 'Greg Brown'
  s.source           = { :git => "https://github.com/gk-brown/HTTP-RPC.git", :tag => s.version.to_s }
  s.platform     = :ios, '8.0'
  s.requires_arc = true
  s.source_files = 'Client/Objective-C/HTTPRPC/*.{h,m}'
end

Pod::Spec.new do |s|
  s.name             = 'HTTPRPC'
  s.version          = '3.6'
  s.summary          = 'Lightweight multi-platform REST client'
  s.description      = <<-DESC
    HTTP-RPC is an open-source framework for simplifying development of REST applications.
    It allows developers to access REST-based web services using a convenient, RPC-like
    metaphor while preserving fundamental REST principles such as statelessness and uniform
    resource access.
    DESC
  s.homepage         = 'https://github.com/gk-brown/HTTP-RPC'
  s.license          = 'Apache License, Version 2.0'
  s.author           = 'Greg Brown'
  s.source           = { :git => "https://github.com/gk-brown/HTTP-RPC.git", :tag => s.version.to_s }
  s.platform     = :ios, '8.0'
  s.requires_arc = true
  s.source_files = 'Client/Objective-C/HTTPRPC/*.{h,m}'
end

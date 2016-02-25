Explain, in roughly one paragraph, how authentication and authorization schemes work: 

Authentication is the processing of verifying the identity of a person. Put in a layman’s term, authentication is verifying if the person is someone he/she really claims to be. Authentication can achieved through different schemes. The most basic one is to get the Username and Password from the user and verify it. Other advanced techniques include finger print verification, retina scan, voice recognition and more.

Authorization is what follows authentication. Authorization restricts what resources (API’s, servers etc) this authenticated user is allowed to access.  In more simpler terms, authorization verifies if this user has the authority to access this particular resource. There are different specifications to follow this authorization process and OAuth is one of such widely used authorization framework.

What is OAuth2? Explain, in your own words, client application token flow. 

OAuth 2 is an authorization framework. It enables applications to access user resoruces or accounts on different HTTP service. Examples of HTTP service include websites like Facebook, Twitter, Github etc. 
OAuth 2 deals with authorization wherein it gives the responsibilty of authenticating the user to the HTTP service that hosts that user account. For example, authenticaitng a user in Facebook is the responsbility of Facebook. It is done via username and password. OAuth2 now servs to authorize 3rd party apps to access this authenticated Facebook user account.

Following Steps are involved in the client application token flow (from my understanding of creating the Twitter APP):

1. Application is created in Twitter.com with a valid twitter login. The application has a consumer key and consumer secret. This step ensure the valid user from HTTP service, Twitter has created this application. 
2. With this consumer key and consumer secret, the application now sends a HTTP request to the API server to get the OAuth access token
3. The server now verifies the cosumer secret and consumer key and knows it has been authorized by a particular user with some permissions (This depends on what permission the user grants to this app while creating) and send back the OAuth access token.
4. This token is now appended on the HTTP header and serves as authorization token to access the API’s from the server.

What distinguishes OAuth2 from a scheme like session cookies? (This question is more general than specific to Twitter) 

Session cookies are maintained by the browser. Once a user is authenticated and his crendentials are stored in the cookie, the browser will use these credentials for all subsequent request until the sesssion is valid. Thus these session cookies are vulnerable to different attacks like Cross-Site Request Forgery and Cross-Site Scripting. OAuth2 on the other hand is managed by the client application and it is the client who appends this token in the HTTP header as, Authentication:Bearer. So in my understanding, OAuth2 provided better security as its tokens are hashed efficiently and can’t be forged easily.




























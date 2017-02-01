# zanmi [![Build Status](https://travis-ci.org/zonotope/zanmi.svg?branch=master)](https://travis-ci.org/zonotope/zanmi)

An HTTP identity service based on JWT auth tokens, and built
on [buddy](https://github.com/funcool/buddy). Authenticate users while managing
their passwords and auth tokens independently of the apps or services they use.

zanmi serves auth tokens in response to requests with the correct user
credentials. It manages a self contained password database with configurable
back ends (current support for PostgreSQL and MongoDB) and hashes passwords
with [BCrypt + SHA512](https://en.wikipedia.org/wiki/Bcrypt) before they're
stored. The signing algorithm zanmi signs it's auth tokens with is also
configurable. [RSASSA-PSS](https://en.wikipedia.org/wiki/PKCS_1) is the default,
but
[ECDSA](https://en.wikipedia.org/wiki/Elliptic_Curve_Digital_Signature_Algorithm) and
[SHA512 HMAC](https://en.wikipedia.org/wiki/SHA-2) are also supported.

Other back-end services can then authenticate users by verifying these auth
tokens with the zanmi keypair's public key.

## Project Maturity
zanmi is still alpha software. There are probably bugs, and the api will most
likely change.

## Usage
zanmi is designed to be deployed with SSL/TLS in production. User passwords will
be sent in the clear otherwise.

zanmi is a way to share authentication across many independent services and
front ends. it depends on a database back end and a key pair/secret to sign
tokens. Both the database and the algorithm used to sign tokens is configurable.
The supported databases are PostgreSQL (default) and MongoDB, and the supported
token signing algorithms are RSASSA-PSS (default), ECDSA, and SHA512 HMAC. Both
RSA-PSS and ECDSA require paths to both a public and private key file, and
SHA512 HMAC needs a secret supplied in the server config.

To try it out in development:

* download the latest [release jar](https://github.com/zonotope/zanmi/releases/download/0.1.0-alpha0/zanmi-0.1.0-alpha0-standalone.jar)

* generate an RSA keypair with [openssl](https://www.openssl.org/) by running
  the following in a terminal, where `<keypair path>` is some path of your
  choosing:

  ```sh
  mkdir -p <keypair path>
  openssl genrsa -out <keypair path>/priv.pem 2048
  openssl rsa -pubout -in <keypair path>/priv.pem -out <keypair path>/pub.pem
  ```

* run either a PostgreSQL or MongoDB server including a database user that can
  update and delete databases.

* download and edit the
  [example config](https://github.com/zonotope/zanmi/blob/master/config.edn.example)
  by adding a random string as an api key and replacing the keypair paths and
  database credentials with your own.

* initialize the database by running:

  ```sh
  ZANMI_CONFIG=<path to edited config> java -jar <zanmi release jar> --init-db
  ```

* zanmi was designed to be run with ssl, but we'll turn it off for now. Start
  the server by running:

  ```sh
  ZANMI_CONFIG=<path to edited config> java -jar <zanmi release jar> --skip-ssl
  ```

The server will be listening at `localhost:8686` (unless you changed the port in
the config). zanmi speaks json by default, but can also use transit/json if you
set the request's accept/content-type headers.

### Clients
There is a [Clojure zanmi client](https://github.com/zonotope/zanmi-client), and
since zanmi is just a plain http server, clients for other languages should be
easy to write as long as those languages have good http and jwt libraries.

We'll use [cURL](https://curl.haxx.se) to make requests to the running server to
be as general as possible. Enter the following commands into a new terminal
window.

#### Registering User Profiles
Send a `post` request to the profiles url with your credentials to register a
new user:

```bash
curl -XPOST --data "profile[username]=gwcarver&profile[password]=pulverized peanuts" localhost:8686/profiles/
```

zanmi uses [zxcvbn-clj](https://github.com/zonotope/zxcvbn-clj) to validate
password strength, so simple passwords like "p4ssw0rd" will fail validations and
an error response will be returned. The server will respond with an auth token
if the password is strong enough and the username isn't already taken.

#### Authenticating Users
Clients send user credentials with http basic auth to zanmi servers to
authenticate against existing user profiles. To verify that you have the right
password, send a `post` request to the user's profile auth url with the
credentials formatted `"username:password"` after the `-u` command switch to
cURL:

```bash
curl -XPOST -u "gwcarver:pulverized peanuts" localhost:8686/profiles/gwcarver/auth
```

The server will respond with an auth token if the credentials are correct.

#### Resetting Passwords

##### With the Current Password
To reset the user's password, send a `put` request to the user's profile url
with the existing credentials through basic auth and the new password in the
request body:

```bash
curl -XPUT -u "gwcarver:pulverized peanuts" --data "profile[password]=succulent sweet potatos" localhost:8686/profiles/gwcarver
```

The server will respond with a new auth token if your credentials are correct
and the new password is strong enough according to zxcvbn

##### With a Reset Token
zanmi also supports resetting user passwords if they've forgotten them. First,
create a JWT of the hash `{"username" : <username value> }` sha512 signed with
the api-key from the zanmi config and send a `post` request with that JWT as the
`ZanmiAppToken` in the authorization header to get a reset token
for that user:

```bash
curl -XPOST -H "Authorization: ZanmiAppToken <jwt>" localhost:8686/profiles/gwcarver/reset
```

Then send the same `put` request as resetting with the current password above,
but change the authorization header value to `ZanmiResetToken <reset token>`,
where `<reset token>`.

```bash
curl -XPUT -H "Authorization: ZanmiResetToken <reset token>" --data "profile[password]=succulent sweet potatos" localhost:8686/profiles/gwcarver
```

The [clojure zanmi client](https://github.com/zonotope/zanmi-client) builds the
authorization JWT used to get the reset token automatically.

In production, your back-end application should request the reset token and send
that to the user's email, or some other trusted channel that will verify their
identity.

#### Removing User Profiles

##### With Valid Credentials
To remove a user's profile from the database, send a delete request to the
users's profile url with the right credentials:

```bash
curl -XDELETE -u "gwcarver:succulent sweet potatos" localhost:8686/profiles/gwcarver
```

## Deployment

### Configuration
See the
[example config](https://github.com/zonotope/zanmi/blob/master/config.edn.example)
for configuration options. The easiest way to configure zanmi is to edit the
example config file linked above and set the `ZANMI_CONFIG` environment variable
when running the relase jar. You can also set the configuration from the
environment. See `environ` in `src/zanmi/config.clj` for environment variable
overrides.

### Database Initialization
PostgreSQL and MongoDB are the only databases supported currently, but pull
requests are welcome.

There are no migrations. Use the `--init-db` command line switch to set up the
database and database tables.

## FAQs
* What about OAuth?
  - While I do have plans to implement an OAuth2 provider based on zanmi
    eventually, full OAuth2 support was a little overkill for my immediate use
    case. I wrote zanmi because I primarily wanted to (1) share user
    authentication among decoupled services and (2) isolate user password
    storage from all the other application data.

* How do users log out?
  - zanmi only supports password database management and stateless
    authentication, so there is no session management. Client applications are
    free to manage their own separate sessions and use that in combination with
    the `:iat` and `:updated` fields of the auth tokens to support logout.

* What's with the name?
  - "zanmi" means [friend](https://github.com/cemerick/friend) or
    [buddy](https://github.com/funcool/buddy) in Haitian Creole.

## TODO
* Full OAuth2 implementation
* Configurable password hashing schemes (support for pbkdf2, scrypt, etc)
* Password database back ends for MySQL, Cassandra, etc.
* More configurable password strength validations
* Shared sessions (possibly with Redis)
* Validate zanmi configuration map with clojure.spec
* More tests!

## Contributing
Pull requests welcome!

### Developing
First install [leiningen](http://leiningen.org/) and clone the repository.

#### Setup

When you first clone this repository, run:

```sh
lein setup
```

This will create files for local configuration, and prep your system
for the project.

#### Environment

To begin developing, start with a REPL.

```sh
lein repl
```

Then load the development environment.

```clojure
user=> (dev)
:loaded
```

Run `go` to initiate and start the system.

```clojure
dev=> (go)
:started
```

By default this creates a web server at <http://localhost:8686>.

When you make changes to your source files, use `reset` to reload any
modified files and reset the server.

```clojure
dev=> (reset)
:reloading (...)
:resumed
```

#### Testing

Testing is fastest through the REPL, as you avoid environment startup
time.

```clojure
dev=> (test)
...
```

But you can also run tests through Leiningen.

```sh
lein test
```

## Legal

Copyright © 2016 ben lamothe.

Distributed under the MIT License

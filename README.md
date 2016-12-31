# zanmi
[![Clojars Project](https://img.shields.io/clojars/v/zanmi.svg)](https://clojars.org/zanmi)

An HTTP identity service built
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

## Usage
zanmi is designed to be deployed with SSL/TLS in production. User passwords will
be sent in the clear otherwise.

zanmi depends on a database back end and a key pair/secret to sign tokens. Both
the database and the algorithm used to sign tokens is configurable. The
supported databases are PostgreSQL (default) and MongoDB, and the supported
token signing algorithms are RSASSA-PSS (default), ECDSA, and SHA512 HMAC. Both
RSA-PSS and ECDSA require paths to both a public and private key file, and
SHA512 HMAC needs a secret supplied in the server config.

To try it out in development:

* clone this repository and run `lein setup` from the repository directory.

* generate an RSA keypair with [openssl](https://www.openssl.org/) by running
  the following from the repository directory:

  ```sh
  mkdir -p dev/resources/keypair/
  openssl genrsa -out dev/resources/keypair/priv.pem 2048
  openssl rsa -pubout -in dev/resources/keypair/priv.pem -out dev/resources/keypair/pub.pem
  ```

* run either a PostgreSQL or MongoDB server including a database user that can
  update and delete databases, and update the dev-config in the `dev/dev.clj`
  file with the database/database user credentials.

* run `lein repl` from the repository directory. Then, from the repl, run:

  1. `(dev)` to load the development environment.
  2. `(init)` to load the system
  3. `(require '[zanmi.boundary.database :as db])` to load database setup fns
  4. `(db/initialize! (:db system))` to set up the database.
  5. `(start)` to start the server.

The development server will be listening at `localhost:8686`. zanmi speaks json
by default, but can also use transit/json if you set the request's
accept/content-type headers.

We'll use [cURL](https://curl.haxx.se) to make requests to the dev server. Enter
the following commands into a new terminal.

#### Registering User Profiles
Send a `post` request to the profiles url with your credentials to register a
new user:

```bash
curl -XPOST --data "username=gwcarver&password=pulverized peanuts" localhost:8686/profiles/
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

##### With Current Password
To reset the user's password, send a `put` request to the user's profile url
with the existing credentials through basic auth and the new password in the
request body:

```bash
curl -XPUT -u "gwcarver:pulverized peanuts" --data "new-password=succulent sweet potatos" localhost:8686/profiles/gwcarver
```

The server will respond with a new auth token if your credentials are correct
and the new password is strong enough according to zxcvbn

#### Removing User Profiles
To remove a user's profile from the database, send a delete request to the
users's profile url with the right credentials:

```bash
curl -XDELETE -u "gwcarver:succulent sweet potatos" localhost:8686/profiles/gwcarver
```

## Deployment
TBD

### Configuration

See `dev-config` in `dev/dev.clj` for configuration options. You can also set
the configuration from the environment. See `environ` in `src/zanmi/config.clj`
for environment variable overrides.

### Database Initialization
PostgreSQL and MongoDB are the only databases supported currently, but pull
requests are welcome.

There are no migrations. Call `zanmi.boundary.database/initialize!` on the
running system's database component to set up the database and database tables.

## FAQs
* How do users log out?
  - zanmi only supports password database management and stateless
    authentication, so there is no session management. Client applications are
    free to manage their own separate sessions and use that in combination with
    the `:iat` and `:updated` fields of the auth tokens to support logout.

* What's with the name?
  - "zanmi" means [friend](https://github.com/cemerick/friend) or
    [buddy](https://github.com/funcool/buddy) in Haitian Creole.

## TODO
* Configurable password hashing schemes (support for pbkdf2, scrypt, etc)
* Password database back ends for MySQL, Cassandra, etc.
* More configurable password strength validations
* Shared sessions (possibly with Redis)
* Validate zanmi configuration map with clojure.spec

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

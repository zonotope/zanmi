# zanmi
An HTTP authentication service built
on [buddy](https://github.com/funcool/buddy). Authenticate users while managing
their passwords and auth tokens independently of the apps or services they use.

zanmi serves auth tokens in response to requests with the correct user
credentials. It manages a self contained password database with configurable
back ends (current support for PostgreSQL and MongoDB) and hashes passwords with
BCrypt + SHA512.

## Usage
zanmi is designed to be deployed with SSL in production. User passwords will be
sent in the clear otherwise.

zanmi depends on a database back end and a key pair/secret to sign tokens. Both
the database and the algorithm used to sign tokens is configurable. The
supported databases are PostgreSQL (default) and MongoDB, and the supported
token signing algorithms are RSASSA-PSS (default), ECDSA, and SHA512 HMAC. Both
RSA-PSS and ECDSA require paths to both a public and private key file, and
SHA512 HMAC needs a secret supplied in the server config.

To try it out in development, first clone this repository and run `lein setup`
from the repository directory.

Then generate an RSA keypair by running: `mkdir -p dev/resources/keypair/ &&
openssl genrsa -out dev/resources/keypair/priv.pem 2048 && openssl rsa -pubout
-in dev/resources/keypair/priv.pem -out dev/resources/keypair/pub.pem`.

Next run either a PostgreSQL or MongoDB server including a database user that
can update and delete databases, then update the dev-config in the `dev/dev.clj`
file with the database user credentials.

Now run `lein repl` from the repository directory. Then, from the repl, run:

1. `(dev)` to load the development environment.
2. `(init)` to load the system
3. `(require '[zanmi.boundary.database :as db])` for database setup
4. `(db/initialize! (:db system))` to set up the database.
5. `(start)` to start the server.

The development server will be listening at `localhost:8686`. zanmi speaks json
by default, but can also use transit/json if you set the requests
accept/content-type headers.

#### Registering User Profiles
Post `{"username" : "name", "password" : "pass"}` to `localhost:8686/profiles/`.

#### Authenticating Users
Post `{"password" : "pass"}` to `localhost:8686/profiles/username/auth` to get a
signed JWT auth token

#### Resetting Passwords

##### With Current Password
Put `{"password" : "old password", "new-password" : "new"}` to
`localhost:8686/profiles/username/` to change the password in the database and get
a new auth token

##### With Reset Token
First get a reset token by signing a request with the client api key and posting
it to `localhost:8686/profiles/username/reset`, and send the reset token to the
user in an email or something. Then, put `{:reset-token token "new-password" :
"password"}` to `localhost:8686/profiles/username/`.

#### Removing User Profiles
Delete `{"password" : "pass"}` to `localhost:8686/profiles/username` to delete the
user's database record.

## Deployment
TBD

### Configuration

See `dv-config` in `dev/dev.clj` for configuration options.

### Database Initialization
PostgreSQL and MongoDB are the only databases supported currently, but pull
requests are welcome.

There are no migrations. Call `zanmi.boundary.database/initialize!` on the
running system's database component.

## FAQs
* How do users log out?
  - zanmi only supports password database management and stateless
    authentication, so there is no session management. Client applications are
    free to manage their own separate sessions and use that in combination with
    the `:iat` and `:updated` fields of the auth tokens to support logout.

* What's with the name?
  - "zanmi" means [friend](https://github.com/cemerick/friend) or
    [buddy](https://github.com/funcool/buddy) in Haitian Creole.

## Plans
* Configurable password hashing schemes
* Password database back ends for MySQL, Cassandra, etc.
* Shared sessions (possibly with Redis)
* Read configuration from edn files

## Contributing
Pull requests welcome!

### Developing

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

Copyright © 2016 ben lamothe

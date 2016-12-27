# zanmi

An HTTP authentication service built
on [buddy](https://github.com/funcool/buddy). Authenticate users while managing
their passwords and auth tokens independently of the apps or services they use.

## Usage

zanmi is designed to be deployed with SSL in production. User passwords will be
sent in the clear otherwise. It serves auth tokens from requests with the user's
credentials. It manages a self contained password database with configurable
back ends (current support for PostgreSQL and MongoDB) and hashes passwords with
BCrypt + SHA512.

To try it out in development, clone this repository and run either postgres or
mongo locally. Update the dev config in `dev/dev.clj` with the database
credentials and run `lein repl` from the repository directory. Then, from the
repl, run:

1. `(dev)` to load the development environment.
2. `(init)` to load the system
3. `(require '[zanmi.boundary.database :as db])`
4. `(db/initialize! (:db system))` to set up the database.
5. `(start)` to start the server.

The development server will be listening at `localhost:8686`.

#### Registering User Profiles

Post `{:username "name", :password "pass"}` to `<zanmi-url>/profiles/`.

#### Authenticating Users

Post `{:password "pass"}` to `<zanmi-url>/profiles/username/auth` to get a
signed JWT auth token

#### Resetting Passwords

##### With Current Password

Put `{:password "old password" :new-password "new"}` to
`<zanmi-url>/profiles/username/` to change the password in the database and get
a new auth token

##### With Reset Token

First get a reset token by signing a request with the client api key and posting
it to `<zanmi-url>/profiles/username/reset`, and send the reset token to the
user in an email or something. Then, put `{:reset-token token :new-password
"password"}` to `<zanmi-url>/profiles/username/`.

#### Removing User Profiles

Delete `{:password "pass"}` to `<zanmi-url>/profiles/username` to delete the
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

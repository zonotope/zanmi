# zanmi

HTTP authentication service built on [buddy](https://github.com/funcool/buddy).

## Usage

### Registering User Profiles

### Authenticating Users

### Resetting Passwords

#### With Current Password

#### With Reset Token

### Removing User Profiles

## Deployment

### Configuration

### Database Initialization

## FAQs
* How do users log out?
  - zanmi only supports password database management and stateless
    authentication, so there is no session management. Client applications are
    free to manage their own separate sessions and use that session in
    combination with the `:iat` and `:updated` fields of the auth token to
    support logout.

* What's with the name?
  - "zanmi" means [friend](https://github.com/cemerick/friend)
    or [buddy](https://github.com/funcool/buddy) in Haitian Creole.

## Plans

## Contributing

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

By default this creates a web server at <http://localhost:3000>.

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

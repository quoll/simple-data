# homework

Homework assignment for Guaranteed Rate.

## Usage

To run tests:

```
lein test
```

To run the basic program that loads and prints:
```
lein run [--store STORE_NAME] [--file DATA_FILE]
```

To run the service:
```
lein with-profile service run [--port PORT] [--store STORE_NAME]
```

The store name is used in principle, but is just a label for internal data and may be ignored.

## Shell test

A simple integration test is also available:
```
./test.sh
```

## License

Copyright © 2022 Paula Gearon

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.


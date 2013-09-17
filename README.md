# conclujon

Conclujon is meant to help you with writing [living documentation](http://specificationbyexample.com/key_ideas.html). It is inspired by [Concordion](http://concordion.org/) but tries to fit the functional Clojure world better by doing some things differently. It is also in a very early stage (you might call it a proof of concept), so expect things to change often.

## Why HTML?

If you are familiar with [Specification by Example](http://specificationbyexample.com/), you might remember the part about automating validation without changing specification. While there is no lack of great libraries for automating BDD style examples, I found translating requirements into [Concordion's](http://concordion.org) HTML-based test format a lot more flexible. As living documentation system, it simply offers a lot more flexibility than the alternatives.

## Why not Concordion?

[Concordion](http://concordion.org/) is well integrated into the Java test ecosystem. That's a good thing. It does make implementing fixtures in Clojure awkward at best, thought. Conclujon on the other hand is conceived with Clojure in mind and tries not to make any further assumption about your application. Being able to rely on [Enlive](https://github.com/cgrand/enlive) for the heavy lifting makes reinventing the wheel a lot less tedious, too.

## Usage

Conclujon test cases consist of two parts which are joined together by the library by conventions. You express your test case in HTML:

```html
<html>
  <head>
    <style type="text/css">
      .failed { text-decoration: line-through; color: black; background-color: red; }
      .passed { color: black; background-color: green; }
      .actual { color: black; background-color: red; padding-left: 5px; }
    </style>
  </head>
  <body>
    <p>
      A person named <em data-conclujon:assoc-input=":name">Stranger</em> will be 
      greeted with <em data-conclujon:assert-equals="greeting">Hello Stranger!</em>.<br/>
    </p>
      A person named <em data-conclujon:assoc-input=":name">Nils</em> will be
      greeted with <em data-conclujon:assert-equals="greeting">Hello Niels!</em>.
    </p>
  </body>
</html>
```

Then you execute the test case against your application by writing fixtures in Clojure:

```clojure
(ns spec.hello-world-test
  (:require [conclujon.core :refer [conclujon-spec]]))

(defn greeting 
  "The fixture called by the data-conclujon:assoc-equals='greeting' element.
   Usually, this will act as an adapter for your application code (e.g. calling
   a greeting service, scraping a website, etc.)"
   [{name :name}] 
   (str "Hello " name "!"))

;; This tells conclujon to look for and execute an HTML specification.
;; For fixtures in the spec.hello-world-test namespace, it expects the
;; specification to be found at spec/hello_world_test.html
(conclujon-spec basic-test)
```

As the `conclujon-spec` macro generates a `clojure.test`-compatible test case, you can simply run your test cases with by calling `lein test`. The results will be a failed test and an HTML file `target/conclujon/spec/hello_world_test.html` with the following content:

```html
<html>
  <head>
    <style type="text/css">
      .failed { text-decoration: line-through; color: black; background-color: red; }
      .passed { color: black; background-color: green; }
      .actual { color: black; background-color: red; padding-left: 5px; }
    </style>
  </head>
  <body>
    <p>
      A person named <em data-conclujon:assoc-input=":name">Stranger</em> will be 
      greeted with <em class="passed" data-conclujon:assert-equals="greeting">Hello Stranger!</em>.<br/>
    </p>
      A person named <em data-conclujon:assoc-input=":name">Nils</em> will be
      greeted with <em class="failed" data-conclujon:assert-equals="greeting">Hello Niels!</em><span class="actual">(actual: Hello Nils!)</span>.
    </p>
  </body>
</hmtl>
```

## How does Conclujon execute the specification?

When parsing an HTML specification, Conclujon will create batches and execute them seperately according to the following rules:

1.  Build a map from all `data-conclujon:assoc-input` attributes occuring before the next `data-conclujon:assert-equals` attribute
    by association the value of the `assoc-input` attribute with the content of the node it is attached to.
2.  In turn, resolve the values of all following `data-conclujon:assert-equals` attributes as function in the current namespace and
    call them with the map generated in step 1.
3.  Compare the result of the function call to the content of the name the `assert-equals` attribute is attached to.
4.  If content and result match, add the `passed` class to the corresponding node. If content and result do not match, add the `failed`
    class to the corresponding name and insert another node with the actual result behind it.
5.  Write out the modified HTML files and let the `clojure.test` test fail if any assertion failed.

## What's next

Apparently, the current feature set of Conclujon is minimal. The next things on the roadmap are:

* Add unit tests and travis-ci configuration
* Tabular scenarios
* Support for destructuring in assert-equals (similar to the `concordion:execute="#var = methodCall"` directive)

## License

Copyright © 2013 Nils Wloka

Distributed under the Eclipse Public License, the same as Clojure.

JAVA = PATH="/opt/homebrew/opt/openjdk/bin:$$PATH"

test-python:
	cd python && source .venv/bin/activate && python -m pytest tests/ -v

test-clojure:
	cd clojure && $(JAVA) clj -M:test -e \
		"(require '[clojure.test :as t]) \
		 (require '[circle-db.constructs-test]) \
		 (require '[circle-db.storage-test]) \
		 (require '[circle-db.eavt-test]) \
		 (t/run-all-tests #\"circle-db.*\")"

test: test-python test-clojure

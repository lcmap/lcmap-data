(ns lcmap.data.scene
  ""
  (:require [clojure.tools.logging :as log]
            [clojure.core.memoize]
            [clojurewerkz.cassaforte.cql :as cql]
            [clojurewerkz.cassaforte.query :as query]
            [clojure.data.json :as json])
  (:refer-clojure :exclude [find]))

;; How do we decide where to look for scenes?
;; ... is this part of the system config?
;; ... is this part of the tile-spec?
;; ... what happens if we store the same scene projected differently?

;; We have to look in one place...
;; ...similar ttile-specs.

(defn column-names
  ""
  [db]
  (let [session (:session db)
        kn (:scene-keyspace db)
        tn (:scene-table db)
        columns (cql/describe-columns session kn tn)]
    (->> columns
         (map :column_name)
         (map keyword)
         (into []))))

(def column-names-memo
  (clojure.core.memoize/lu column-names))

(defn find
  ""
  [db scene]
  (let [session (:session db)
        kn (:scene-keyspace db)
        tn (:scene-table db)
        scene- (select-keys scene (column-names-memo db))]
    (cql/use-keyspace session kn)
    (cql/select session tn (query/where scene))))

(defn save
  ""
  [db scene]
  (let [session (:session db)
        kn (:scene-keyspace db)
        tn (:scene-table db)
        scene- (select-keys scene (column-names-memo db))]
    (cql/use-keyspace session kn)
    (cql/insert-async session tn scene)))

(defn save-band
  ""
  [db band]
  (let [scene (select-keys band (column-names-memo db))
        global_metadata (band :global_metadata)]
    (save db (assoc scene :global_metadata (json/json-str global_metadata)))))

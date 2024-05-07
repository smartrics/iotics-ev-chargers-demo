package com.iotics.demo;

import static com.iotics.demo.EvAnalytics.runQuery;

public class EvTwinInPeterborough {

    private static final String aTwin = """
            PREFIX ev: <https://data.iotics.com/ont/ev#>
            PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

            SELECT ?subject ?predicate ?object
            WHERE {
              ?subject ?predicate ?object .
              ?subject rdfs:label "IEC 62196-2 Type 2,ID:68607" .
            }""";

    public static void main(String[] args) throws Exception {
        runQuery(aTwin);
    }
}
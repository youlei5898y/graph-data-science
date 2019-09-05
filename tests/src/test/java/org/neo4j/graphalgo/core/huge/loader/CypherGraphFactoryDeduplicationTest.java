/*
 * Copyright (c) 2017-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.graphalgo.core.huge.loader;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.neo4j.graphalgo.TestDatabaseCreator;
import org.neo4j.graphalgo.api.Graph;
import org.neo4j.graphalgo.core.DeduplicateRelationshipsStrategy;
import org.neo4j.graphalgo.core.GraphLoader;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.kernel.internal.GraphDatabaseAPI;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.neo4j.graphalgo.GraphHelper.collectTargetWeights;

class CypherGraphFactoryDeduplicationTest {

    private static GraphDatabaseService db;

    private static int id1;
    private static int id2;

    @BeforeAll
    static void setUp() {
        db = TestDatabaseCreator.createTestDatabase();

        db.execute(
                "MERGE (n1 {id: 1}) " +
                "MERGE (n2 {id: 2}) " +
                "CREATE (n1)-[:REL {weight: 4}]->(n2) " +
                "CREATE (n2)-[:REL {weight: 10}]->(n1) " +
                "RETURN id(n1) AS id1, id(n2) AS id2").accept(row -> {
            id1 = row.getNumber("id1").intValue();
            id2 = row.getNumber("id2").intValue();
            return true;
        });
    }

    @AfterAll
    static void tearDown() {
        db.shutdown();
    }

    @Test
    void testLoadCypher() {
        String nodes = "MATCH (n) RETURN id(n) AS id";
        String rels = "MATCH (n)-[r]-(m) RETURN id(n) AS source, id(m) AS target, r.weight AS weight";

        Graph graph = new GraphLoader((GraphDatabaseAPI) db)
                .withLabel(nodes)
                .withRelationshipType(rels)
                .withDeduplicateRelationshipsStrategy(DeduplicateRelationshipsStrategy.SKIP)
                .load(CypherGraphFactory.class);

        assertEquals(2, graph.nodeCount());
        assertEquals(1, graph.degree(graph.toMappedNodeId(id1), Direction.OUTGOING));
        assertEquals(1, graph.degree(graph.toMappedNodeId(id2), Direction.OUTGOING));
    }

    @Test
    void testLoadCypherDuplicateRelationshipsWithWeights() {
        String nodes = "MATCH (n) RETURN id(n) AS id";
        String rels = "MATCH (n)-[r]-(m) RETURN id(n) AS source, id(m) AS target, r.weight AS weight";

        Graph graph = new GraphLoader((GraphDatabaseAPI) db)
                .withLabel(nodes)
                .withRelationshipType(rels)
                .withRelationshipWeightsFromProperty("weight", 1.0)
                .withDeduplicateRelationshipsStrategy(DeduplicateRelationshipsStrategy.SKIP)
                .load(CypherGraphFactory.class);

        double[] weights = collectTargetWeights(graph, graph.toMappedNodeId(id1));
        assertEquals(1, weights.length);
        assertTrue(weights[0] == 10.0 || weights[0] == 4.0);
    }

    @ParameterizedTest
    @CsvSource({"SUM, 14.0", "MAX, 10.0", "MIN, 4.0"})
    void testLoadCypherDuplicateRelationshipsWithWeightsAggregation(
            DeduplicateRelationshipsStrategy deduplicateRelationshipsStrategy,
            double expectedWeight) {
        String nodes = "MATCH (n) RETURN id(n) AS id";
        String rels = "MATCH (n)-[r]-(m) RETURN id(n) AS source, id(m) AS target, r.weight AS weight";

        Graph graph = new GraphLoader((GraphDatabaseAPI) db)
                .withLabel(nodes)
                .withRelationshipType(rels)
                .withRelationshipWeightsFromProperty("weight", 1.0)
                .withDeduplicateRelationshipsStrategy(deduplicateRelationshipsStrategy)
                .load(CypherGraphFactory.class);

        double[] weights = collectTargetWeights(graph, graph.toMappedNodeId(id1));
        assertArrayEquals(new double[]{expectedWeight}, weights);
    }
}
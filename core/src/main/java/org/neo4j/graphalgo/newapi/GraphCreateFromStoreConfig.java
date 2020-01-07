/*
 * Copyright (c) 2017-2020 "Neo4j,"
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
package org.neo4j.graphalgo.newapi;

import org.immutables.value.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.neo4j.graphalgo.NodeProjections;
import org.neo4j.graphalgo.PropertyMapping;
import org.neo4j.graphalgo.PropertyMappings;
import org.neo4j.graphalgo.RelationshipProjections;
import org.neo4j.graphalgo.annotation.Configuration;
import org.neo4j.graphalgo.annotation.Configuration.ConvertWith;
import org.neo4j.graphalgo.annotation.ValueClass;
import org.neo4j.graphalgo.core.CypherMapWrapper;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@ValueClass
@Configuration("GraphCreateFromStoreConfigImpl")
public interface GraphCreateFromStoreConfig extends GraphCreateConfig {

    @NotNull String NODE_PROJECTION_KEY = "nodeProjection";
    @NotNull String RELATIONSHIP_PROJECTION_KEY = "relationshipProjection";

    @Override
    @Configuration.Parameter
    @ConvertWith("org.neo4j.graphalgo.AbstractNodeProjections#fromObject")
    NodeProjections nodeProjection();

    @Override
    @Configuration.Parameter
    @ConvertWith("org.neo4j.graphalgo.AbstractRelationshipProjections#fromObject")
    RelationshipProjections relationshipProjection();

    @Override
    @Configuration.Ignore
    @Nullable default String nodeQuery() {
        return null;
    }

    @Override
    @Configuration.Ignore
    @Nullable default String relationshipQuery() {
        return null;
    }

    @Value.Check
    @Configuration.Ignore
    default GraphCreateFromStoreConfig withNormalizedPropertyMappings() {
        PropertyMappings nodeProperties = nodeProperties();
        PropertyMappings relationshipProperties = relationshipProperties();

        verifyProperties(
            nodeProperties.stream().map(PropertyMapping::propertyKey).collect(Collectors.toSet()),
            nodeProjection().allProperties(),
            "node"
        );

        verifyProperties(
            relationshipProperties.stream().map(PropertyMapping::propertyKey).collect(Collectors.toSet()),
            relationshipProjection().allProperties(),
            "relationship"
        );

        if (nodeProperties.hasMappings() || relationshipProperties.hasMappings()) {
            return ImmutableGraphCreateFromStoreConfig
                .builder()
                .from(this)
                .nodeProjection(nodeProjection().addPropertyMappings(nodeProperties))
                .nodeProperties(PropertyMappings.of())
                .relationshipProjection(relationshipProjection().addPropertyMappings(relationshipProperties))
                .relationshipProperties(PropertyMappings.of())
                .build();
        }
        return this;
    }

    @Configuration.Ignore
    default void verifyProperties(
        Set<String> propertiesFromMapping,
        Set<String> propertiesFromProjection,
        String type
    ) {
        Set<String> propertyIntersection = new HashSet<>(propertiesFromMapping);
        propertyIntersection.retainAll(propertiesFromProjection);

        if (!propertyIntersection.isEmpty()) {
            throw new IllegalArgumentException(String.format(
                "Incompatible %s projection and %s property specification. Both specify properties named %s",
                type, type, propertyIntersection
            ));
        }
    }

    static GraphCreateFromStoreConfig emptyWithName(String userName, String graphName) {
        return ImmutableGraphCreateFromStoreConfig.of(userName, graphName, NodeProjections.empty(), RelationshipProjections.empty());
    }

    static GraphCreateFromStoreConfig of(
        String userName,
        String graphName,
        Object nodeProjections,
        Object relationshipProjections,
        CypherMapWrapper config
    ) {
        GraphCreateFromStoreConfig GraphCreateFromStoreConfig = new GraphCreateFromStoreConfigImpl(
            nodeProjections,
            relationshipProjections,
            graphName,
            userName,
            config
        );
        return GraphCreateFromStoreConfig.withNormalizedPropertyMappings();
    }

    static GraphCreateFromStoreConfig fromProcedureConfig(String username, CypherMapWrapper config) {
        NodeProjections nodeProjections = NodeProjections.fromObject(CypherMapWrapper.failOnNull(
            NODE_PROJECTION_KEY,
            config.get(NODE_PROJECTION_KEY, (Object) NodeProjections.empty())
        ));

        RelationshipProjections relationshipProjections = RelationshipProjections.fromObject(CypherMapWrapper.failOnNull(
            RELATIONSHIP_PROJECTION_KEY,
            config.get(RELATIONSHIP_PROJECTION_KEY, (Object) RelationshipProjections.empty())
        ));

        relationshipProjections.projections().values().forEach(relationshipProjection -> {
            if (relationshipProjection.properties().mappings().size() > 1) {
                throw new IllegalArgumentException(
                    "Implicit graph loading does not allow loading multiple relationship properties per relationship type");
            }
        });

        GraphCreateFromStoreConfig GraphCreateFromStoreConfig = new GraphCreateFromStoreConfigImpl(
            nodeProjections,
            relationshipProjections,
            IMPLICIT_GRAPH_NAME,
            username,
            config
        );
        return GraphCreateFromStoreConfig.withNormalizedPropertyMappings();
    }
}
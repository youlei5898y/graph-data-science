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
package org.neo4j.graphalgo.core.loading;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.io.layout.DatabaseFile;
import org.neo4j.kernel.impl.store.NeoStores;
import org.neo4j.kernel.impl.store.NodeStore;
import org.neo4j.kernel.impl.store.RecordStore;
import org.neo4j.kernel.impl.store.format.RecordFormat;
import org.neo4j.kernel.impl.store.format.RecordFormats;
import org.neo4j.kernel.impl.store.record.NodeRecord;

public final class NodeStoreScanner extends AbstractStorePageCacheScanner<NodeRecord> {

    public static final AbstractStorePageCacheScanner.Access<NodeRecord> NODE_ACCESS = new Access<NodeRecord>() {
        @Override
        public RecordStore<NodeRecord> store(final NeoStores neoStores) {
            return neoStores.getNodeStore();
        }

        @Override
        public RecordFormat<NodeRecord> recordFormat(final RecordFormats formats) {
            return formats.node();
        }

        @Override
        public String storeFileName() {
            return DatabaseFile.NODE_STORE.getName();
        }

        @Override
        public AbstractStorePageCacheScanner<NodeRecord> newScanner(
                final GraphDatabaseService api,
                final int prefetchSize) {
            return new NodeStoreScanner(prefetchSize, api);
        }
    };

    private NodeStoreScanner(final int prefetchSize, final GraphDatabaseService api) {
        super(prefetchSize, api, NODE_ACCESS);
    }

    @Override
    NodeStore store() {
        return (NodeStore) super.store();
    }
}

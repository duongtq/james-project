/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mailrepository.cassandra;

import java.util.Set;

import javax.inject.Inject;

import org.apache.james.mailrepository.api.MailRepositoryUrl;
import org.apache.james.mailrepository.api.MailRepositoryUrlStore;

import com.github.steveash.guavate.Guavate;

public class CassandraMailRepositoryUrlStore implements MailRepositoryUrlStore {

    private final UrlsDao urlsDao;

    @Inject
    public CassandraMailRepositoryUrlStore(UrlsDao urlsDao) {
        this.urlsDao = urlsDao;
    }

    @Override
    public void addUrl(MailRepositoryUrl url) {
        urlsDao.addUrl(url).join();
    }

    @Override
    public Set<MailRepositoryUrl> retrieveUsedUrls() {
        return urlsDao.retrieveUsedUrls()
            .join()
            .collect(Guavate.toImmutableSet());
    }

    @Override
    public boolean contains(MailRepositoryUrl url) {
        return urlsDao.retrieve(url)
            .join()
            .isPresent();
    }
}

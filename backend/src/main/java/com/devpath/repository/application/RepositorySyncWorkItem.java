package com.devpath.repository.application;

import com.devpath.repository.domain.Repository;
import com.devpath.repository.domain.RepositorySyncJob;

record RepositorySyncWorkItem(RepositorySyncJob job, Repository repository) {}

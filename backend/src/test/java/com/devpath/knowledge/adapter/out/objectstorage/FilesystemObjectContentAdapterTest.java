package com.devpath.knowledge.adapter.out.objectstorage;

import com.devpath.shared.infrastructure.FilesystemObjectContentAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FilesystemObjectContentAdapterTest {
    @TempDir Path root;
    @Test void referencesStayOpaqueAndOwnerScoped() {
        var adapter=new FilesystemObjectContentAdapter(root.toString());
        UUID owner=UUID.randomUUID(),other=UUID.randomUUID(),document=UUID.randomUUID(),version=UUID.randomUUID();
        String reference=adapter.put(owner,document,version,"chunks/0.md","private content");
        assertThat(reference).startsWith("object://"+owner+"/");
        assertThat(adapter.read(owner,reference)).isEqualTo("private content");
        assertThatThrownBy(() -> adapter.read(other,reference)).isInstanceOf(IllegalArgumentException.class);
        adapter.deleteVersion(owner,document,version);
        assertThatThrownBy(() -> adapter.read(owner,reference)).isInstanceOf(IllegalStateException.class);
    }
}

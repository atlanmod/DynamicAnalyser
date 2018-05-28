package com.tblf.parsing;

import org.apache.commons.io.FileUtils;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.query.IQueryEngine;
import org.hawk.core.runtime.ModelIndexerImpl;
import org.hawk.core.util.DefaultConsole;
import org.hawk.emf.metamodel.EMFMetaModelResourceFactory;
import org.hawk.emf.model.EMFModelResourceFactory;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.graph.updater.GraphMetaModelUpdater;
import org.hawk.graph.updater.GraphModelUpdater;
import org.hawk.localfolder.LocalFolder;
import org.hawk.neo4j_v2.Neo4JDatabase;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

public class HawkQueryTest {
    private IModelIndexer modelIndexer;
    private IQueryEngine queryEngine;

    @Before
    public void setUp() throws Exception {
        File file = new File("neo4j");
        if (file.exists()) {
            FileUtils.deleteDirectory(file);
        }
        modelIndexer = new ModelIndexerImpl("indexer", new File("neo4j/"), null, new DefaultConsole());
        modelIndexer.addMetaModelResourceFactory(new EMFMetaModelResourceFactory());

        EMFModelResourceFactory emfModelResourceFactory = new EMFModelResourceFactory();
        modelIndexer.addModelResourceFactory(emfModelResourceFactory);

        IGraphDatabase db = (new Neo4JDatabase());
        db.run(modelIndexer.getParentFolder(), modelIndexer.getConsole());

        modelIndexer.setDB(db, true);
        modelIndexer.setMetaModelUpdater(new GraphMetaModelUpdater());

        File ecore = new File("src/test/resources/hawk/Ecore.ecore");
        File mm = new File("src/test/resources/hawk/java.ecore");

        modelIndexer.setMetaModelUpdater(new GraphMetaModelUpdater());
        modelIndexer.addModelUpdater(new GraphModelUpdater());

        modelIndexer.registerMetamodels(ecore);
        modelIndexer.registerMetamodels(mm);

        queryEngine = new EOLQueryEngine();
        modelIndexer.addQueryEngine(queryEngine);

    }

    @Test
    public void checkEOLQuery() throws Throwable {

        LocalFolder localFolder = new LocalFolder();
        localFolder.init("src/test/resources/hawk/models/", modelIndexer);
        localFolder.run();

        modelIndexer.addVCSManager(localFolder, true);

        modelIndexer.init(1000, 512 * 1000);

        waitForSync(modelIndexer, () -> null);

        Object o = queryEngine.query(modelIndexer, "return MethodDeclaration.all.size();", new HashMap<>());
        Assert.assertEquals(4164, o);
    }

    @Test
    public void checkEOLQuery2() throws Throwable {

        LocalFolder localFolder = new LocalFolder();
        localFolder.init("src/test/resources/hawk/models/", modelIndexer);
        localFolder.run();

        modelIndexer.addVCSManager(localFolder, true);

        modelIndexer.init(1000, 512 * 1000);

        waitForSync(modelIndexer, () -> null);

        Object o = queryEngine.query(modelIndexer, "return ClassDeclaration.all.size();", new HashMap<>());
        Assert.assertEquals(1272, o);
    }

    @After
    public void tearDown() throws Exception {
        modelIndexer.delete();
    }

    protected void waitForSync(IModelIndexer indexer, final Callable<?> r) throws Throwable {
        final Semaphore sem = new Semaphore(0);
        final SyncEndListener changeListener = new SyncEndListener(r, sem);
        indexer.addGraphChangeListener(changeListener);
        if (!sem.tryAcquire(600, TimeUnit.SECONDS)) {
            fail("Synchronization timed out");
        } else {
            indexer.removeGraphChangeListener(changeListener);
            if (changeListener.getThrowable() != null) {
                throw changeListener.getThrowable();
            }
        }
    }
}


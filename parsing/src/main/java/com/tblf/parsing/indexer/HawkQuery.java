package com.tblf.parsing.indexer;

import com.tblf.parsing.Query;
import com.tblf.utils.Configuration;
import org.eclipse.emf.common.util.URI;
import org.eclipse.modisco.java.composition.javaapplication.Java2File;
import org.eclipse.modisco.kdm.source.extension.ASTNodeSourceRegion;
import org.hawk.core.IModelIndexer;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.runtime.ModelIndexerImpl;
import org.hawk.core.util.DefaultConsole;
import org.hawk.emf.metamodel.EMFMetaModelResourceFactory;
import org.hawk.emf.model.EMFModelResourceFactory;
import org.hawk.emfresource.impl.LocalHawkResourceImpl;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.epsilon.emc.wrappers.GraphNodeWrapper;
import org.hawk.graph.updater.GraphMetaModelUpdater;
import org.hawk.graph.updater.GraphModelUpdater;
import org.hawk.localfolder.LocalFolder;
import org.hawk.neo4j_v2.Neo4JDatabase;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.fail;

//TODO
public class HawkQuery implements Query, AutoCloseable {
    private IModelIndexer modelIndexer;
    private LocalHawkResourceImpl localHawkResource;

    private static final Logger LOGGER = Logger.getAnonymousLogger();

    public HawkQuery(File modelDirectory) {
        File file = new File(Configuration.getProperty("indexDirectory"));

        IGraphDatabase db = new Neo4JDatabase();
        db.run(file, new DefaultConsole());

        modelIndexer = new ModelIndexerImpl("indexer", file, null, new DefaultConsole());

        modelIndexer.addMetaModelResourceFactory(new EMFMetaModelResourceFactory());
        modelIndexer.addModelResourceFactory(new EMFModelResourceFactory());
        modelIndexer.addQueryEngine(new EOLQueryEngine());
        modelIndexer.setMetaModelUpdater(new GraphMetaModelUpdater());
        modelIndexer.addModelUpdater(new GraphModelUpdater());
        modelIndexer.setDB(db, true);
        try {
            modelIndexer.init(0, 0);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not initialize the indexer", e);
        }

        /* Loading the metamodels */
        File ecore = new File("src/main/resources/hawk/Ecore.ecore");
        File mm = new File("src/main/resources/hawk/java.ecore");
        File jApplicationmm = new File("src/main/resources/hawk/javaApplication.ecore");
        File kdmSourceExtension = new File("src/main/resources/hawk/kdmSourceExtension.ecore");
        File kdm = new File("src/main/resources/hawk/kdm.ecore");

        try {
            modelIndexer.registerMetamodels(ecore);
            modelIndexer.registerMetamodels(mm);
            modelIndexer.registerMetamodels(kdm);
            modelIndexer.registerMetamodels(kdmSourceExtension);
            modelIndexer.registerMetamodels(jApplicationmm);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load metamodel", e);
        }

        /* Adding the repository */
        LocalFolder localFolder = new LocalFolder();
        try {
            localFolder.init(modelDirectory.getAbsolutePath(), modelIndexer);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not index the model", e);
        }
        localFolder.run();
        modelIndexer.addVCSManager(localFolder, true);

        localHawkResource = new LocalHawkResourceImpl(URI.createURI(file.getAbsolutePath()), modelIndexer, false, null, null);
    }

    @Override
    public Collection<ASTNodeSourceRegion> queryLine(int lineStart, int lineEnd, Java2File java2File) {
        waitForSync(modelIndexer, () -> {
                    Object value = modelIndexer.getKnownQueryLanguages().get("org.hawk.epsilon.emc.EOLQueryEngine")
                    .query(modelIndexer,
                            "return null;"
                            ,null);

                    LOGGER.fine(value.toString());
                    return null;
        });

        //TODO
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<ASTNodeSourceRegion> queryPosition(int startPos, int endPos, Java2File java2File) {
        waitForSync(modelIndexer, () -> modelIndexer.getKnownQueryLanguages().get("org.hawk.epsilon.emc.EOLQueryEngine")
                .query(modelIndexer,
                        ""
                        ,null));

        //TODO
        return Collections.EMPTY_LIST;
    }

    protected void waitForSync(IModelIndexer indexer, final Callable<?> r) {
        final Semaphore sem = new Semaphore(0);
        final SyncEndListener changeListener = new SyncEndListener(r, sem);
        indexer.addGraphChangeListener(changeListener);
        try {
            if (!sem.tryAcquire(60, TimeUnit.SECONDS)) {
                fail("Synchronization timed out");
            } else {
                indexer.removeGraphChangeListener(changeListener);
                if (changeListener.getThrowable() != null)
                    throw changeListener.getThrowable();
            }
        } catch (Throwable e) {
            LOGGER.log(Level.WARNING, "Error with indexer synchronization", e);
        }
    }

    @Override
    public void close() throws Exception {
        modelIndexer.shutdown(IModelIndexer.ShutdownRequestType.ONLY_LOCAL);
    }
}


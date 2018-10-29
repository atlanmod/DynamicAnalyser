package com.tblf.parsing.indexer;

import com.tblf.parsing.queries.Query;
import com.tblf.utils.Configuration;
import org.apache.commons.io.IOUtils;
import org.eclipse.emf.common.util.URI;
import org.eclipse.modisco.java.composition.javaapplication.Java2File;
import org.eclipse.modisco.kdm.source.extension.ASTNodeSourceRegion;
import org.hawk.core.IModelIndexer;
import org.hawk.core.IStateListener;
import org.hawk.core.graph.IGraphDatabase;
import org.hawk.core.query.InvalidQueryException;
import org.hawk.core.query.QueryExecutionException;
import org.hawk.core.runtime.ModelIndexerImpl;
import org.hawk.core.util.DefaultConsole;
import org.hawk.emf.metamodel.EMFMetaModelResourceFactory;
import org.hawk.emf.model.EMFModelResourceFactory;
import org.hawk.emfresource.impl.LocalHawkResourceImpl;
import org.hawk.epsilon.emc.EOLQueryEngine;
import org.hawk.graph.updater.GraphMetaModelUpdater;
import org.hawk.graph.updater.GraphModelUpdater;
import org.hawk.localfolder.LocalFolder;
import org.hawk.neo4j_v2.Neo4JDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

//TODO
public class HawkQuery implements Query, AutoCloseable {
    private IModelIndexer modelIndexer;
    private LocalHawkResourceImpl localHawkResource;

    private static final Logger LOGGER = Logger.getAnonymousLogger();
    private static final Semaphore sem = new Semaphore(0);

    private GraphModelUpdater graphModelUpdater;


    public HawkQuery(File modelDirectory) {
        File file = new File(modelDirectory, Configuration.getProperty("indexDirectory"));

        IGraphDatabase db = new Neo4JDatabase();
        db.run(file, new DefaultConsole());

        modelIndexer = new ModelIndexerImpl("indexer", file, null, new DefaultConsole());

        modelIndexer.addMetaModelResourceFactory(new EMFMetaModelResourceFactory());
        modelIndexer.addModelResourceFactory(new EMFModelResourceFactory());
        modelIndexer.addQueryEngine(new EOLQueryEngine());
        graphModelUpdater = new GraphModelUpdater();
        modelIndexer.setMetaModelUpdater(new GraphMetaModelUpdater());
        modelIndexer.addModelUpdater(graphModelUpdater);
        modelIndexer.setDB(db, true);
        try {
            modelIndexer.init(0, 0);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not initialize the indexer", e);
        }

        /* Loading the metamodels */
        try {
            modelIndexer.registerMetamodels(
                    loadOrCreateModel("hawk/Ecore.ecore"),
                    loadOrCreateModel("hawk/java.ecore"),
                    loadOrCreateModel("hawk/smm.ecore")
            );
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

    private File loadOrCreateModel(String resourceUrl) throws IOException {
        //"hawk/Ecore.ecore"
        File model = new File("src/main/resources/" + resourceUrl);
        if (!model.exists()) {
            model.getParentFile().mkdirs();
            model.createNewFile();
            IOUtils.copy(getClass().getClassLoader().getResourceAsStream(resourceUrl), new FileOutputStream(model));
        }

        return model;
    }

    @Override
    public Collection<ASTNodeSourceRegion> queryLine(int lineStart, int lineEnd, Java2File java2File) {
        waitForSync(modelIndexer, () -> modelIndexer.getKnownQueryLanguages().get("org.hawk.epsilon.emc.EOLQueryEngine")
                .query(modelIndexer,
                        ""
                        , null));
        //TODO
        return Collections.EMPTY_LIST;
    }

    @Override
    public Collection<ASTNodeSourceRegion> queryPosition(int startPos, int endPos, Java2File java2File) {
        waitForSync(modelIndexer, () -> modelIndexer.getKnownQueryLanguages().get("org.hawk.epsilon.emc.EOLQueryEngine")
                .query(modelIndexer,
                        ""
                        , null));
        //TODO
        return Collections.EMPTY_LIST;
    }

    public Object queryWithInputEOLQuery(String query) {

        /*Object value = waitForSync(modelIndexer, () ->
                modelIndexer.getKnownQueryLanguages().get("org.hawk.epsilon.emc.EOLQueryEngine").query(modelIndexer, query, null));*/

        Object value = null;
        if (modelIndexer.getCompositeStateListener().getCurrentState() != IStateListener.HawkState.RUNNING)
            waitForSync(modelIndexer, () -> null);

        try {
            value = modelIndexer.getKnownQueryLanguages().get("org.hawk.epsilon.emc.EOLQueryEngine").query(modelIndexer, query, null);
        } catch (InvalidQueryException | QueryExecutionException e) {
            e.printStackTrace();
        }
        //sem.release();

        return value;

    }


    private Object waitForSync(IModelIndexer indexer, final Callable<?> r) {

        final SyncEndListener changeListener = new SyncEndListener(r, sem);
        indexer.addGraphChangeListener(changeListener);
        try {
            if (!sem.tryAcquire(180, TimeUnit.SECONDS)) {
                throw new RuntimeException("Synchronization timed out");
            } else {
                indexer.removeGraphChangeListener(changeListener);
                if (changeListener.getThrowable() != null)
                    throw changeListener.getThrowable();
            }
        } catch (Throwable e) {
            LOGGER.log(Level.WARNING, "Error with indexer synchronization", e);
        }

        return changeListener.getReturnValue();
    }

    @Override
    public void close() throws Exception {
        modelIndexer.shutdown(IModelIndexer.ShutdownRequestType.ONLY_LOCAL);
    }
}


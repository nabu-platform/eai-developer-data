package be.nabu.eai.developer.plugin;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import be.nabu.eai.api.NamingConvention;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.api.EntryContextMenuProvider;
import be.nabu.eai.developer.managers.util.SimpleProperty;
import be.nabu.eai.developer.managers.util.SimplePropertyUpdater;
import be.nabu.eai.developer.util.EAIDeveloperUtils;
import be.nabu.eai.module.data.model.DataModelArtifact;
import be.nabu.eai.module.data.model.DataModelManager;
import be.nabu.eai.module.data.model.DataModelType;
import be.nabu.eai.module.http.server.HTTPServerArtifact;
import be.nabu.eai.module.http.server.HTTPServerManager;
import be.nabu.eai.module.http.virtual.VirtualHostArtifact;
import be.nabu.eai.module.http.virtual.VirtualHostManager;
import be.nabu.eai.module.jdbc.context.GenerateDatabaseScriptContextMenu;
import be.nabu.eai.module.jdbc.dialects.h2.H2Dialect;
import be.nabu.eai.module.jdbc.pool.JDBCPoolArtifact;
import be.nabu.eai.module.jdbc.pool.JDBCPoolManager;
import be.nabu.eai.module.swagger.provider.SwaggerProvider;
import be.nabu.eai.module.swagger.provider.SwaggerProviderManager;
import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.module.web.application.WebApplicationManager;
import be.nabu.eai.module.web.application.WebFragment;
import be.nabu.eai.module.web.application.api.TargetAudience;
import be.nabu.eai.module.web.component.WebComponent;
import be.nabu.eai.module.web.component.WebComponentManager;
import be.nabu.eai.module.web.resources.WebComponentContextMenu;
import be.nabu.eai.repository.EAINode;
import be.nabu.eai.repository.EAIResourceRepository;
import be.nabu.eai.repository.ProjectImpl;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.Node;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.eai.server.RemoteServer;
import be.nabu.jfx.control.tree.Tree;
import be.nabu.jfx.control.tree.TreeCell;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.property.api.Property;
import be.nabu.libs.resources.ResourceUtils;
import be.nabu.libs.resources.api.ManageableContainer;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.services.jdbc.api.SQLDialect;
import be.nabu.libs.types.DefinedTypeResolverFactory;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.DefinedTypeRegistry;
import be.nabu.libs.types.properties.CollectionNameProperty;
import be.nabu.utils.mime.impl.FormatException;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.layout.VBox;

public class DataProjectContextMenu implements EntryContextMenuProvider {

	public static final String MISC_FOLDER = "shared";
	public static final String WEB_FOLDER = "web";
	
	@Override
	public MenuItem getContext(Entry entry) {
		// if we can add things to it, allow creation of a project
		if (!entry.isLeaf() && entry instanceof RepositoryEntry) {
			boolean isPartOfProject = false;
			Entry lookup = entry;
			while (lookup != null) {
				if (lookup.isProject()) {
					isPartOfProject = true;
					break;
				}
				lookup = lookup.getParent();
			}
			if (!isPartOfProject) {
				MenuItem item = new MenuItem("Create Project");
				item.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						SimplePropertyUpdater updater = new SimplePropertyUpdater(true, new LinkedHashSet<Property<?>>(Arrays.asList(
							new SimpleProperty<String>("Name", String.class, true)
						)));
						EAIDeveloperUtils.buildPopup(MainController.getInstance(), updater, "Create New Project", new EventHandler<ActionEvent>() {
							@Override
							public void handle(ActionEvent arg0) {
								String name = updater.getValue("Name");
								if (name != null) {
									try {
										String repoName = NamingConvention.LOWER_CAMEL_CASE.apply(name);
										RepositoryEntry createDirectory = ((RepositoryEntry) entry).createDirectory(repoName);
										ProjectImpl project = new ProjectImpl();
										project.setName(name);
										createDirectory.setProject(project);
										
										Tree<Entry> control = MainController.getInstance().getRepositoryBrowser().getControl();
										// we need to refresh the parent we created it in
										TreeCell<Entry> treeCell = control.getTreeCell(entry.getParent() == null ? control.rootProperty().get() : control.resolve(entry.getId().replace(".", "/")));
										if (treeCell != null) {
											treeCell.refresh();
										}
										else {
											MainController.getInstance().getRepositoryBrowser().refresh();
										}
										try {
											MainController.getInstance().getAsynchronousRemoteServer().reload(createDirectory.getId());
										}
										catch (Exception e) {
											e.printStackTrace();
										}
										MainController.getInstance().getCollaborationClient().created(createDirectory.getId(), "Created project");
										
										generateProject(name, createDirectory);
									}
									catch (IOException e) {
										MainController.getInstance().notify(e);
									}
								}
							}
						});
						
					}
				});
				item.setGraphic(MainController.loadGraphic("folder-project.png"));
				return item;
			}
		}
		return null;
	}
	
	private void generateProject(String name, RepositoryEntry projectEntry) {
		Tab newTab = MainController.getInstance().newTab("Generating " + name);
		newTab.setClosable(false);
		
		VBox box = new VBox();
		Label action = new Label("Generating...");
		action.setPadding(new Insets(10));
		ProgressBar bar = new ProgressBar();
		box.setAlignment(Pos.CENTER);
		box.getChildren().addAll(action, bar);
		
		newTab.setContent(box);
		
		// do this asynchronously
		new Thread(new Runnable() {
			@Override
			public void run() {
				int stepCounter = 1;
				int totalSteps = 7;
				Exception exception = null;
				try {
					set(action, bar, "Creating HTTP server", stepCounter++, totalSteps);
					HTTPServerArtifact server = getOrCreateHTTPServer(projectEntry);
					set(action, bar, "Creating Virtual Host", stepCounter++, totalSteps);
					VirtualHostArtifact host = getOrCreateVirtualHost(projectEntry, server);
					set(action, bar, "Creating API component", stepCounter++, totalSteps);
					WebComponent apiComponent = getOrCreateAPIComponent(projectEntry, "site");
					set(action, bar, "Creating swagger", stepCounter++, totalSteps);
					SwaggerProvider swagger = getOrCreateSwagger(projectEntry, "site");
					// by creating the model before the database, it should automatically get picked up and auto-managed?
					set(action, bar, "Creating data model", stepCounter++, totalSteps);
					getOrCreateDatamodel(projectEntry);
					set(action, bar, "Creating database", stepCounter++, totalSteps);
					JDBCPoolArtifact jdbc = getOrCreateJDBCPool(projectEntry);
					set(action, bar, "Creating application", stepCounter++, totalSteps);
					WebApplication application = getOrCreateWebApplication(projectEntry, "site", TargetAudience.CUSTOMER);
					
					set(action, bar, "Reloading...", stepCounter++, totalSteps);
				} 
				catch (Exception e) {
					MainController.getInstance().notify(e);
					exception = e;
				}
				finally {
					try {
						MainController.getInstance().getAsynchronousRemoteServer().reload(projectEntry.getId());
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								MainController.getInstance().getRepositoryBrowser().refresh();
							}
						});
						if (exception == null) {
							Platform.runLater(new Runnable() {
								@Override
								public void run() {
									box.getChildren().remove(bar);
									action.setText("Project " + name + " successfully created");
									box.getChildren().addAll(MainController.loadGraphic("dialog/success.png"));
								}
							});
						}
						else {
							Platform.runLater(new Runnable() {
								@Override
								public void run() {
									box.getChildren().remove(bar);
									action.setText("Failed to create project " + name);
									box.getChildren().addAll(MainController.loadGraphic("dialog/failed.png"));
								}
							});
						}
					}
					catch (Exception e) {
						e.printStackTrace();
					}
					newTab.setClosable(true);
				}
			}
		}).start();
	}
	
	public static WebApplication getOrCreateWebApplication(RepositoryEntry projectEntry, String applicationName, TargetAudience audience) {
		try {
			RepositoryEntry entry = getFolder(projectEntry, applicationName);
			
			Entry child = entry.getChild("application");
			if (child == null) {
				VirtualHostArtifact host = getOrCreateVirtualHost(projectEntry, getOrCreateHTTPServer(projectEntry));
				JDBCPoolArtifact jdbc = getOrCreateJDBCPool(projectEntry);
				
				// check if there are already applications on the host, if so, we can't take their paths
				List<String> paths = new ArrayList<String>();
				for (WebApplication existing : entry.getRepository().getArtifacts(WebApplication.class)) {
					if (host.equals(existing.getConfig().getVirtualHost())) {
						paths.add(existing.getServerPath());
					}
				}
				
				RepositoryEntry applicationEntry = ((RepositoryEntry) entry).createNode("application", new WebApplicationManager(), true);
				applicationEntry.getNode().setName(NamingConvention.UPPER_TEXT.apply(applicationName));
				applicationEntry.saveNode();
				
				child = applicationEntry;
				WebApplication application = new WebApplication(applicationEntry.getId(), applicationEntry.getContainer(), applicationEntry.getRepository());
				application.getConfig().setHtml5Mode(true);
				application.getConfig().setVirtualHost(host);
				String path = "/";
				switch (audience) {
					case MANAGER: 
						path += "manage";
					break;
					case BUSINESS:
						path += "admin";
					break;
				}
				while (paths.contains(path)) {
					if (path.equals("/")) {
						path += entry.getName();
					}
					// this will generate 1, 11, 111 instead of 1,2,3 but the edge case is rare enough that it doesn't matter?
					else {
						path += "1";
					}
				}
				application.getConfig().setPath(path);
				application.getConfig().setWebFragments(new ArrayList<WebFragment>(Arrays.asList(getOrCreateAPIComponent(projectEntry, applicationName), getOrCreateSwagger(projectEntry, applicationName))));
				
				// add the cms all, task manage etc etc
				// we add any component flagged as being standard for your target audience
				for (WebComponent potential : MainController.getInstance().getRepository().getArtifacts(WebComponent.class)) {
					if (audience.equals(potential.getConfig().getAudience()) && !application.getConfig().getWebFragments().contains(potential)) {
						application.getConfig().getWebFragments().add(potential);
					}
				}
				
				// update the cms configuration to have the correct JDBC
				ComplexContent configuration = application.getConfigurationFor(".*", (ComplexType) DefinedTypeResolverFactory.getInstance().getResolver().resolve("nabu.cms.core.configuration"));
				if (configuration == null) {
					configuration = ((ComplexType) DefinedTypeResolverFactory.getInstance().getResolver().resolve("nabu.cms.core.configuration")).newInstance();
				}
				configuration.set("connectionId", jdbc.getId());
				
				// set the security restrictions by default
				switch(audience) {
					case BUSINESS: 
						configuration.set("security/allowedRoles", new ArrayList<String>(Arrays.asList("business")));
					break;
					case MANAGER:
						configuration.set("security/allowedRoles", new ArrayList<String>(Arrays.asList("manager")));
					break;
					case CUSTOMER:
						configuration.set("passwordRegex", "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}");
					break;
				}
				configuration.set("caseInsensitive", true);
				configuration.set("masterdata/preloadedCategories", new ArrayList<String>(Arrays.asList("language", "attachmentGroup")));
				
				application.putConfiguration(configuration, null, false);
				
				new WebApplicationManager().save(applicationEntry, application);
				
				// fix the page builder template, this will already fix a lot of stuff
				ManageableContainer<?> publicDirectory = (ManageableContainer<?>) ResourceUtils.mkdirs(applicationEntry.getContainer(), EAIResourceRepository.PUBLIC);
				ManageableContainer<?> privateDirectory = (ManageableContainer<?>) ResourceUtils.mkdirs(applicationEntry.getContainer(), EAIResourceRepository.PRIVATE);
				WebComponentContextMenu.copyPageWithCms(applicationEntry, publicDirectory, privateDirectory);
			}
			return (WebApplication) child.getNode().getArtifact();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private void set(Label action, ProgressBar bar, String string, double i, double totalSteps) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				action.setText(string);
				bar.setProgress(i / totalSteps);
			}
		});
	}
	
	public static RepositoryEntry getMiscFolder(RepositoryEntry projectEntry) {
		return getFolder(projectEntry, MISC_FOLDER);
	}
	
	public static RepositoryEntry getFolder(RepositoryEntry projectEntry, String name) {
		// we make or use a folder for misc artifacts
		Entry folder = projectEntry.getChild(name);
		if (folder == null) {
			try {
				folder = projectEntry.createDirectory(name);
			}
			catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return (RepositoryEntry) folder;
	}

	public static SwaggerProvider getOrCreateSwagger(RepositoryEntry projectEntry, String applicationName) {
		try {
			RepositoryEntry miscFolder = getFolder(projectEntry, applicationName);
			Entry child = miscFolder.getChild("swagger");
			if (child == null) {
				RepositoryEntry swaggerEntry = miscFolder.createNode("swagger", new SwaggerProviderManager(), true);
				child = swaggerEntry;
				SwaggerProvider swagger = new SwaggerProvider(swaggerEntry.getId(), swaggerEntry.getContainer(), swaggerEntry.getRepository());
				swagger.getConfig().setBasePath("/api/otr");
				new SwaggerProviderManager().save(swaggerEntry, swagger);
			}
			return (SwaggerProvider) child.getNode().getArtifact();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static DataModelArtifact getOrCreateDatamodel(RepositoryEntry projectEntry) {
		try {
			RepositoryEntry miscFolder = getFolder(projectEntry, "database");
			Entry child = miscFolder.getChild("model");
			if (child == null) {
				RepositoryEntry dataModelEntry = miscFolder.createNode("model", new DataModelManager(), true);
				child = dataModelEntry;
				DataModelArtifact model = new DataModelArtifact(dataModelEntry.getId(), dataModelEntry.getContainer(), dataModelEntry.getRepository());
				model.getConfig().setType(DataModelType.DATABASE);
				new DataModelManager().save(dataModelEntry, model);
			}
			return (DataModelArtifact) child.getNode().getArtifact();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static WebComponent getOrCreateAPIComponent(RepositoryEntry projectEntry, String applicationName) {
		try {
			RepositoryEntry miscFolder = getFolder(projectEntry, applicationName);
			Entry child = miscFolder.getChild("api");
			if (child == null) {
				RepositoryEntry componentEntry = miscFolder.createNode("api", new WebComponentManager(), true);
				child = componentEntry;
				componentEntry.getNode().setName(applicationName + " API");
				componentEntry.getNode().setTags(new ArrayList<String>(Arrays.asList(applicationName + " API")));
				componentEntry.saveNode();
				WebComponent component = new WebComponent(componentEntry.getId(), componentEntry.getContainer(), componentEntry.getRepository());
				component.getConfig().setPath("/api/otr");
				new WebComponentManager().save(componentEntry, component);
			}
			return (WebComponent) child.getNode().getArtifact();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static VirtualHostArtifact getOrCreateVirtualHost(RepositoryEntry projectEntry, HTTPServerArtifact server) {
		VirtualHostArtifact host = getApplicationArtifact(projectEntry, VirtualHostArtifact.class);
		if (host == null) {
			try {
				RepositoryEntry miscFolder = getFolder(projectEntry, MISC_FOLDER);
				// build virtual host
				RepositoryEntry hostEntry = miscFolder.createNode("host", new VirtualHostManager(), true);
				host = new VirtualHostArtifact(hostEntry.getId(), hostEntry.getContainer(), hostEntry.getRepository());
				host.getConfig().setServer(server);
				new VirtualHostManager().save(hostEntry, host);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return host;
	}
	
	private static <T extends Artifact> T getApplicationArtifact(Entry entry, Class<T> clazz) {
		for (T potential : entry.getRepository().getArtifacts(clazz)) {
			if (potential.getId().startsWith(entry.getId() + ".")) {
				return potential;
			}
		}
		return null;
	}
	
	public static HTTPServerArtifact getOrCreateHTTPServer(RepositoryEntry projectEntry) {
		try {
			List<Integer> currentPorts = new ArrayList<Integer>();
			HTTPServerArtifact server = null;
			// we want to check if there is already an http server in your project that we can use
			for (HTTPServerArtifact potential : projectEntry.getRepository().getArtifacts(HTTPServerArtifact.class)) {
				if (potential.getConfig().getPort() != null) {
					currentPorts.add(potential.getConfig().getPort());
				}
				if (potential.getConfig().isEnabled() && potential.getId().startsWith(projectEntry.getId() + ".")) {
					server = potential;
				}
			}
			if (server == null) {
				int port = 8080;
				// get the first available port
				while (currentPorts.indexOf(port) >= 0) {
					port++;
				}
				RepositoryEntry miscFolder = getFolder(projectEntry, MISC_FOLDER);
				RepositoryEntry serverEntry = miscFolder.createNode("server", new HTTPServerManager(), true);
				server = new HTTPServerArtifact(serverEntry.getId(), serverEntry.getContainer(), projectEntry.getRepository());
				// build http server
				server.getConfig().setPort(port);
				server.getConfig().setEnabled(true);
				new HTTPServerManager().save(serverEntry, server);
			}
			return server;
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static JDBCPoolArtifact getOrCreateJDBCPool(RepositoryEntry projectEntry) {
		// now we want to add configuration for the jdbc pool as well
		JDBCPoolArtifact jdbc = getApplicationArtifact(projectEntry, JDBCPoolArtifact.class);
		if (jdbc == null) {
			try {
				RepositoryEntry entry = getFolder(projectEntry, "database");
				
				RepositoryEntry jdbcEntry = ((RepositoryEntry) entry).createNode("connection", new JDBCPoolManager(), true);
				jdbc = new JDBCPoolArtifact(jdbcEntry.getId(), jdbcEntry.getContainer(), jdbcEntry.getRepository());
				
				/**
				 * jdbc:h2:[file:][<path>]<databaseName>
			     * jdbc:h2:~/test
			     * jdbc:h2:file:/data/sample
				 * jdbc:h2:file:C:/data/sample (Windows only)
				 */
				String property = System.getProperty("user.home", ".");
				// we use the id as the name
				jdbc.getConfig().setJdbcUrl("jdbc:h2:file:" + property.replace("\\", "/") + "/" + jdbc.getId());
				// no extensible generics in place?
				Object dialect = H2Dialect.class;
				jdbc.getConfig().setDialect((Class<SQLDialect>) dialect);
				jdbc.getConfig().setDriverClassName("org.h2.Driver");
				jdbc.getConfig().setAutoCommit(false);
				
				jdbc.getConfig().setTranslationGet((DefinedService) entry.getRepository().resolve("nabu.cms.core.providers.translation.jdbc.get"));
				jdbc.getConfig().setTranslationSet((DefinedService) entry.getRepository().resolve("nabu.cms.core.providers.translation.jdbc.set"));
				
				// this should be the main database for the context of your project
				// if other databases are added, we don't want to make sure the cms and other frameworks target this database by default
				// TODO: have "framework" applications (mostly nabu folder for now), which are automatically added to this context
				jdbc.getConfig().setContext(projectEntry.getId());
	
				// autosync all collection-named complex types
				if (jdbc.getConfig().getManagedModels() == null) {
					jdbc.getConfig().setManagedModels(new ArrayList<DefinedTypeRegistry>());
				}
				Map<String, DefinedTypeRegistry> definedModelNames = new HashMap<String, DefinedTypeRegistry>();
				for (DefinedTypeRegistry managed : jdbc.getConfig().getManagedModels()) {
					definedModelNames.put(managed.getId(), managed);
				}
				for (DefinedTypeRegistry registry : entry.getRepository().getArtifacts(DefinedTypeRegistry.class)) {
					for (String namespace : registry.getNamespaces()) {
						for (ComplexType potential : registry.getComplexTypes(namespace)) {
							if (!(potential instanceof DefinedType)) {
								continue;
							}
							String collectionName = ValueUtils.getValue(CollectionNameProperty.getInstance(), potential.getProperties());
							if (collectionName != null) {
								if (!definedModelNames.containsKey(registry.getId())) {
									definedModelNames.put(registry.getId(), registry);
									jdbc.getConfig().getManagedModels().add(registry);
								}
								break;
							}
						}
					}
				}
				// if we have both the model and model version of something, remove the model one (we standardize on emodels as concept)
				for (String id : definedModelNames.keySet()) {
					if (id.contains("emodel") && definedModelNames.containsKey(id.replaceAll("\\bemodel\\b", "model"))) {
						jdbc.getConfig().getManagedModels().remove(definedModelNames.get(id.replaceAll("\\bemodel\\b", "model")));
					}
				}
				new JDBCPoolManager().save((ResourceEntry) entry.getRepository().getEntry(jdbc.getId()), jdbc);
				// we will do a synchronous reload of the jdbc pool because we want to sync the datatypes, which is a server-side operation
				MainController.getInstance().getServer().getRemote().reload(jdbc.getId());
				// make sure we sync ddls
				GenerateDatabaseScriptContextMenu.synchronizeManagedTypes(jdbc);
				jdbcEntry.getNode().setName("Main Database");
				jdbcEntry.saveNode();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return jdbc;
	}
}

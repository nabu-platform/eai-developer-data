/*
* Copyright (C) 2020 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package be.nabu.eai.developer.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import be.nabu.eai.api.NamingConvention;
import be.nabu.eai.developer.MainController;
import be.nabu.eai.developer.plugin.api.ArtifactViewer;
import be.nabu.eai.module.jdbc.pool.JDBCPoolArtifact;
import be.nabu.eai.module.jdbc.pool.JDBCPoolClientUtils;
import be.nabu.eai.module.jdbc.pool.JDBCPoolManager;
import be.nabu.eai.module.jdbc.pool.JDBCPoolUtils;
import be.nabu.eai.module.services.crud.CRUDArtifact;
import be.nabu.eai.module.services.crud.CRUDArtifactManager;
import be.nabu.eai.module.services.crud.CRUDService.CRUDType;
import be.nabu.eai.module.services.crud.provider.CRUDProviderArtifact;
import be.nabu.eai.module.types.structure.StructureManager;
import be.nabu.eai.module.web.application.WebApplication;
import be.nabu.eai.module.web.application.api.DragHandler;
import be.nabu.eai.module.web.application.resource.WebBrowser;
import be.nabu.eai.repository.EAINode;
import be.nabu.eai.repository.api.Entry;
import be.nabu.eai.repository.api.ResourceEntry;
import be.nabu.eai.repository.resources.RepositoryEntry;
import be.nabu.jfx.control.tree.TreeCell;
import be.nabu.jfx.control.tree.TreeItem;
import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.services.api.DefinedService;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.properties.CollectionNameProperty;
import be.nabu.libs.types.structure.DefinedStructure;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import nabu.protocols.jdbc.pool.types.TableDescription;

/**
 * Database file layout by default:
 * 
 * <project>.database.<name>.connection -> connection
 * <project>.database.<name>.types.<tableName>
 * <project>.database.<name>.crud.<tableName>
 */
public class DatabaseView implements ArtifactViewer<JDBCPoolArtifact> {

	private List<TableDescription> listTables;
	private VBox typesBox = new VBox();
	// managed collection names
	private List<String> managedCollectionNames = new ArrayList<String>();
	
	@Override
	public Class<JDBCPoolArtifact> getArtifactClass() {
		return JDBCPoolArtifact.class;
	}

	@Override
	public Node draw(Entry project, JDBCPoolArtifact artifact) {
		VBox box = new VBox();
		ProgressIndicator indicator = new ProgressIndicator();
		box.getChildren().add(indicator);
		ForkJoinPool.commonPool().submit(new Runnable() {
			@Override
			public void run() {
				if (listTables == null) {
					listTables = JDBCPoolClientUtils.listTables(artifact);
				}
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						box.getChildren().clear();
						VBox contentBox = new VBox();
						box.getChildren().add(contentBox);
						contentBox.getChildren().add(typesBox);
						drawTypes(project, artifact);
						if (listTables != null) {
							VBox toAdd = new VBox();
							contentBox.getChildren().add(toAdd);
							BooleanProperty open = DataView.getInstance().getOpen(artifact.getId() + ":add");
							toAdd.visibleProperty().bind(open);
							toAdd.managedProperty().bind(open);
							
							TextField field = new TextField();
							field.setPromptText("Search");
							VBox.setMargin(field, new Insets(10, 20, 10, 40));
							toAdd.getChildren().add(field);
							for (TableDescription table : listTables) {
								boolean found = false;
								for (String name : managedCollectionNames) {
									if (table.getName().equalsIgnoreCase(name)) {
										found = true;
										break;
									}
								}
								if (found) {
									continue;
								}
								HBox box = new HBox();
								box.setAlignment(Pos.CENTER_LEFT);
								
								box.getChildren().add(MainController.loadGraphic("item.png"));
								box.getChildren().add(MainController.loadFixedSizeGraphic("developer-data/table.png", 16, 25));
								
								box.getStyleClass().add("first-child-entry");
								Label label = new Label((table.getSchema() != null ? table.getSchema() + "." : "") + table.getName());
								label.setText(label.getText().toLowerCase());
								label.getStyleClass().add("table-name");
								box.getChildren().add(label);
								label.setMaxWidth(Double.MAX_VALUE);
								HBox.setHgrow(label, Priority.ALWAYS);
								HBox.setMargin(label, DataView.MARGIN_LEFT);
								
								Button button = new Button();
								button.setGraphic(MainController.loadFixedSizeGraphic("developer-data/add.png", 12));
								button.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
									@Override
									public void handle(ActionEvent arg0) {
										// get the parent of the jdbc connection, we typically make the types there
										Entry entry = artifact.getRepository().getEntry(artifact.getId()).getParent();
										if (entry instanceof RepositoryEntry) {
											try {
												Entry typeChild = entry.getChild("types");
												if (typeChild == null) {
													typeChild = ((RepositoryEntry) entry).createDirectory("types");
												}
												Entry crudChild = entry.getChild("crud");
												if (crudChild == null) {
													crudChild = ((RepositoryEntry) entry).createDirectory("crud");
												}
												// make sure they are picked up
												entry.refresh(true);
												
												String name = NamingConvention.LOWER_CAMEL_CASE.apply(table.getName().toLowerCase(), NamingConvention.UNDERSCORE);
												
												// create structure
												StructureManager structureManager = new StructureManager();
												RepositoryEntry createNode = ((RepositoryEntry) typeChild).createNode(name, structureManager, true);
												
												DefinedStructure structure = new DefinedStructure();
												structure.setId(createNode.getId());
												structure.setName(name);
												// likely overwritten by generation
												structure.setProperty(new ValueImpl<String>(CollectionNameProperty.getInstance(), table.getName().toLowerCase()));
												JDBCPoolUtils.toType(structure, table);
												structureManager.save(createNode, structure);
												// set the table name as descriptive name
												createNode.getNode().setName(table.getName().toLowerCase());
												// update the node
												createNode.saveNode();
												((RepositoryEntry) typeChild).refresh(true, false);
												MainController.getInstance().getCollaborationClient().created(structure.getId(), "Added defined type for table: " + table.getName());
												MainController.getInstance().getAsynchronousRemoteServer().reload(structure.getId());
												
												// update jdbc pool
												if (artifact.getConfig().getManagedTypes() == null) {
													artifact.getConfig().setManagedTypes(new ArrayList<DefinedType>());
												}
												artifact.getConfig().getManagedTypes().add((DefinedType) structure);
												// only relink _after_ it is added...
												JDBCPoolUtils.relink(artifact, Arrays.asList(table));
												new JDBCPoolManager().save((ResourceEntry) artifact.getRepository().getEntry(artifact.getId()), artifact);
												
												MainController.getInstance().getCollaborationClient().updated(artifact.getId(), "Added managed type: " + structure.getId());
												MainController.getInstance().getAsynchronousRemoteServer().reload(artifact.getId());
												
												// create crud
												CRUDArtifactManager manager = new CRUDArtifactManager();
												createNode = ((RepositoryEntry) crudChild).createNode(name, manager, true);
												CRUDArtifact crud = new CRUDArtifact(createNode.getId(), createNode.getContainer(), createNode.getRepository());
												// we use the artifact
												crud.getConfig().setConnection(artifact);
												crud.getConfig().setProvider((CRUDProviderArtifact) entry.getRepository().resolve("nabu.services.crud.provider.basic.provider"));
												crud.getConfig().setCoreType(structure);
												crud.getConfig().setChangeTracker((DefinedService) entry.getRepository().resolve("nabu.cms.core.providers.misc.changeTracker"));
												manager.save(createNode, crud);
												((RepositoryEntry) crudChild).refresh(true, false);
												MainController.getInstance().getCollaborationClient().created(crud.getId(), "Added crud for: " + structure.getId());
												MainController.getInstance().getAsynchronousRemoteServer().reload(crud.getId());
												
												// we need to reload the crud to see the new services etc
												MainController.getInstance().getRepository().reload(crud.getId());

												// make sure we open the tree to the correct spot, otherwise the refresh will not properly refresh deeply nested items because of laziness...
												TreeCell<Entry> treeCell = MainController.getInstance().getTree().getTreeCell(MainController.getInstance().getTreeEntry(entry.getId()));
												boolean wasExpanded = treeCell.expandedProperty().get();
												treeCell.expandAll(2);
												
												// redraw the tree
												MainController.getInstance().getRepositoryBrowser().refresh();
												if (!wasExpanded) {
													treeCell.collapseAll();
												}
												
												drawTypes(project, artifact);
												// add no more :|
												toAdd.getChildren().remove(box);
												
												// close the add option, we likely want to add only one table
												DataView.getInstance().getOpen(artifact.getId() + ":add").set(false);
											}
											catch (Exception e) {
												MainController.getInstance().notify(e);
											}
										}
									}
								});
								box.getChildren().add(button);
								toAdd.getChildren().add(box);
							}
							field.textProperty().addListener(new ChangeListener<String>() {
								@Override
								public void changed(ObservableValue<? extends String> arg0, String arg1, String arg2) {
									for (Node child : toAdd.getChildren()) {
										if (child instanceof HBox) {
											// first child is the "item" icon, second is the table icon
											Label label = (Label) ((HBox) child).getChildren().get(2);
											if (arg2 == null || arg2.trim().isEmpty() || label.getText().matches("(?i).*" + arg2.replace("*", ".*") + ".*")) {
												DataView.visible(true, child);
											}
											else {
												DataView.visible(false, child);
											}
										}
									}
								}
							});
						}
					}

				});
			}
		});
		return box;
	}
	
	private void drawTypes(Entry project, JDBCPoolArtifact artifact) {
		typesBox.getChildren().clear();
		managedCollectionNames.clear();
		List<DefinedType> types = artifact.getConfig().getManagedTypes();
		if (types != null) {
			for (DefinedType type : types) {
				// we are currently? only interested in types that live in our project
				// otherwise we might be faced with a ton of types from cms, analysis,...
				if (type == null || !type.getId().startsWith(project.getId() + ".")) {
					continue;
				}
				
				String collectionName = ValueUtils.getValue(CollectionNameProperty.getInstance(), type.getProperties());
				if (collectionName == null) {
					collectionName = type.getName();
				}
				if (collectionName != null) {
					collectionName = NamingConvention.UNDERSCORE.apply(collectionName, NamingConvention.UPPER_CAMEL_CASE);
				}
				managedCollectionNames.add(collectionName);
				VBox childBox = new VBox();
				
				// synchronize button? which direction?
				
				Button crudButton = new Button();
				crudButton.setGraphic(MainController.loadGraphic("crud.png"));
				crudButton.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent arg0) {
						CRUDArtifact crud = getCRUD(project, artifact, type);
						if (crud != null) {
							MainController.getInstance().open(crud.getId());
						}
						else {
							// TODO: create crud!
						}
					}
				});
				
				HBox newArtifactBox = DataView.getInstance().newArtifactBox(type, true, crudButton);
				newArtifactBox.getStyleClass().add("first-child-entry");
				childBox.getChildren().add(newArtifactBox);
				
				newArtifactBox.getChildren().add(1, MainController.loadFixedSizeGraphic("developer-data/table.png", 16, 25));
				
				VBox content = new VBox();
				content.getStyleClass().add("developer-content-box");
				content.managedProperty().bind(DataView.getInstance().getOpen(type.getId()));
				content.visibleProperty().bind(DataView.getInstance().getOpen(type.getId()));
				
				CRUDArtifact crud = getCRUD(project, artifact, type);
				if (crud != null) {
					for (String operation : Arrays.asList("get", "list", "create", "update", "delete")) {
						Artifact result = artifact.getRepository().resolve(crud.getId() + ".services." + operation);
						if (result != null) {
							HBox resultBox = DataView.getInstance().newArtifactBox(result, false);
							resultBox.getStyleClass().add("second-child-entry");
							resultBox.getChildren().add(1, MainController.loadGraphic("vmservice.png"));
							content.getChildren().add(resultBox);
						}
					}
					// not necessary, we have a button above
//					HBox crudBox = DataView.getInstance().newArtifactBox(crud, false);
//					crudBox.getStyleClass().add("second-child-entry");
//					content.getChildren().add(crudBox);
				}
				
				// for each data type you can have 5 services: get/list/delete/update/read
//				content.getChildren().addAll(
//					buildDraggableOperation(type, CRUDType.GET),
//					buildDraggableOperation(type, CRUDType.LIST),
//					buildDraggableOperation(type, CRUDType.CREATE),
//					buildDraggableOperation(type, CRUDType.UPDATE),
//					buildDraggableOperation(type, CRUDType.DELETE)
//				);
				childBox.getChildren().add(content);
				typesBox.getChildren().add(childBox);
			}
		}
	}
	
	private CRUDArtifact getCRUD(Entry project, JDBCPoolArtifact artifact, DefinedType type) {
		for (CRUDArtifact crud : MainController.getInstance().getRepository().getArtifacts(CRUDArtifact.class)) {
			if (crud.getId().startsWith(project.getId() + ".") && crud.getConfig().getCoreType() != null && type.getId().equals(crud.getConfig().getCoreType().getId())) {
				return crud;
			}
		}
		return null;
	}
	
	private HBox buildDraggableOperation(DefinedType type, CRUDType operation) {
		HBox box = new HBox();
		box.getChildren().add(MainController.loadGraphic("item.png"));
		box.getStyleClass().add("second-child-entry");
		box.getChildren().add(MainController.loadGraphic("vmservice.png"));
		Label label = new Label(NamingConvention.UPPER_TEXT.apply(operation.name().toLowerCase(), NamingConvention.UNDERSCORE));
		label.setMaxWidth(Double.MAX_VALUE);
		HBox.setHgrow(label, Priority.ALWAYS);
		label.getStyleClass().add("developer-service");
		HBox.setMargin(label, DataView.MARGIN_LEFT);
		box.getChildren().add(label);
		box.addEventHandler(MouseEvent.DRAG_DETECTED, new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent arg0) {
				WebBrowser.drag(box, new DragHandler() {
					@Override
					public ClipboardContent drop(WebApplication application) {
						System.out.println("-> dropped! " + type + " -> " + operation + " on " + application.getId());
						return null;
					}
				});
			}
		});
		Button button = new Button();
		button.setGraphic(MainController.loadFixedSizeGraphic("right-chevron.png", 12));
		button.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				
			}
		});
		box.getChildren().add(button);
		return box;
	}
	
	@Override
	public String getSectionGraphic() {
		return "developer-data/database.png";
	}
	
	public String getArtifactGraphic() {
//		return "jdbcpool.png";
		return null;
	}

	@Override
	public String getName() {
		return "Databases";
	}

	@Override
	public void create() {
		
	}

	@Override
	public List<Button> getButtons(Entry project, JDBCPoolArtifact artifact) {
		Button button = new Button();
		DataView.getInstance().getOpen(artifact.getId() + ":add").addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> arg0, Boolean arg1, Boolean arg2) {
				if (arg2) {
					button.setGraphic(MainController.loadFixedSizeGraphic("developer-data/cancel.png", 12));
				}
				else {
					button.setGraphic(MainController.loadFixedSizeGraphic("developer-data/add.png", 12));
				}
			}
		});
		button.setGraphic(MainController.loadFixedSizeGraphic("developer-data/add.png", 12));
		button.addEventHandler(ActionEvent.ANY, new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent arg0) {
				BooleanProperty open = DataView.getInstance().getOpen(artifact.getId() + ":add");
				if (open.get()) {
//					button.setGraphic(MainController.loadFixedSizeGraphic("developer-data/add.png", 12));
					DataView.getInstance().getOpen(artifact.getId() + ":add").set(false);
				}
				else {
//					button.setGraphic(MainController.loadFixedSizeGraphic("developer-data/cancel.png", 12));
					DataView.getInstance().getOpen(artifact.getId() + ":add").set(true);
					// make sure the artifact itself is open
					DataView.getInstance().getOpen(artifact.getId()).set(true);
				}
			}
		});
		return Arrays.asList(button);
	}
	
}

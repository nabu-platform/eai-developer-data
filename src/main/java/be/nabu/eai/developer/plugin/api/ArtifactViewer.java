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

package be.nabu.eai.developer.plugin.api;

import java.util.ArrayList;
import java.util.List;

import be.nabu.eai.repository.api.Entry;
import be.nabu.libs.artifacts.api.Artifact;
import javafx.scene.Node;
import javafx.scene.control.Button;

public interface ArtifactViewer<T extends Artifact> {
	public String getName();
	public String getSectionGraphic();
	public Class<T> getArtifactClass();
	public void create();
	public Node draw(Entry projectEntry, T artifact);
	// by default we limit to artifacts within the project
	public default boolean allow(Entry projectEntry, String artifactId) {
		return artifactId.startsWith(projectEntry.getId() + ".");
	}
	public default String getArtifactGraphic() {
		return null;
	}
	public default List<Button> getButtons(Entry project, T artifact) {
		return new ArrayList<Button>();
	}
}

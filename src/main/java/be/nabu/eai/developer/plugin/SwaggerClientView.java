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

import be.nabu.eai.developer.plugin.api.ArtifactViewer;
import be.nabu.eai.module.swagger.client.SwaggerClient;
import be.nabu.eai.repository.api.Entry;
import javafx.scene.Node;

public class SwaggerClientView implements ArtifactViewer<SwaggerClient> {

	@Override
	public String getName() {
		return "Connectors (REST)";
	}

	@Override
	public String getSectionGraphic() {
		return "developer-data/connectors.png";
	}

	@Override
	public Class<SwaggerClient> getArtifactClass() {
		return SwaggerClient.class;
	}

	@Override
	public void create() {
		
	}

	@Override
	public Node draw(Entry project, SwaggerClient artifact) {
		return null;
	}

}

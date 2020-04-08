package com.vmware.tanzu.springcloudgateway;

import io.kubernetes.client.openapi.models.V1ObjectMeta;

public class V1SpringCloudGateway  {

	private V1ObjectMeta metadata;

	public V1SpringCloudGateway(V1ObjectMeta metadata) {
		this.metadata = metadata;
	}

	public V1ObjectMeta getMetadata() {
		return metadata;
	}

	public void setMetadata(V1ObjectMeta metadata) {
		this.metadata = metadata;
	}

}

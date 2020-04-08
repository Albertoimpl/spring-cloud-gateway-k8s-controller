package com.example.springcloudgatewayk8scontroller;

import java.util.Collections;

import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.models.V1LabelSelector;
import io.kubernetes.client.openapi.models.V1NetworkPolicy;
import io.kubernetes.client.openapi.models.V1NetworkPolicyIngressRule;
import io.kubernetes.client.openapi.models.V1NetworkPolicyList;
import io.kubernetes.client.openapi.models.V1NetworkPolicyPeer;
import io.kubernetes.client.openapi.models.V1NetworkPolicySpec;
import io.kubernetes.client.openapi.models.V1ObjectMeta;
import io.kubernetes.client.openapi.models.V1Status;

class NetworkPolicyService {

	private NetworkingV1Api networkingV1Api;

	NetworkPolicyService(NetworkingV1Api networkingV1Api) {
		this.networkingV1Api = networkingV1Api;
	}

	V1NetworkPolicyList listNetworkPolicies() throws ApiException {
		System.out.println("Listing network policies");
		V1NetworkPolicyList networkPolicyList = networkingV1Api.listNamespacedNetworkPolicy(
			"default",
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			null);
		System.out.println("Listed network policies");
		System.out.println(networkPolicyList.getItems().size());
		return networkPolicyList;
	}


	void createNetworkPolicy() throws ApiException {
		System.out.println("Creating network policy");
		String serviceInstanceId = "quack";
		String clientAppLabel = "my-gateway";

		V1NetworkPolicy networkPolicy = new V1NetworkPolicy();
		networkPolicy.setApiVersion("networking.k8s.io/v1");
		networkPolicy.setKind("NetworkPolicy");

		V1ObjectMeta objectMeta = new V1ObjectMeta();
		objectMeta.setName("network-policy-" + serviceInstanceId);
		objectMeta.setNamespace("default"); // I know, I know...
		networkPolicy.setMetadata(objectMeta);

		V1NetworkPolicySpec networkPolicySpec = new V1NetworkPolicySpec();
		V1NetworkPolicyIngressRule networkPolicyIngressRule = new V1NetworkPolicyIngressRule();
		V1NetworkPolicyPeer v1NetworkPolicyPeer = new V1NetworkPolicyPeer();
		V1LabelSelector labelSelector = new V1LabelSelector();
		labelSelector.setMatchLabels(Collections.singletonMap("binds-to", clientAppLabel));
		v1NetworkPolicyPeer.setPodSelector(labelSelector);
		networkPolicyIngressRule.from(Collections.singletonList(v1NetworkPolicyPeer));

		networkPolicySpec.setIngress(Collections.singletonList(networkPolicyIngressRule));

		V1LabelSelector podSelector = new V1LabelSelector();
		podSelector.setMatchLabels(Collections.singletonMap("service-instance-id", serviceInstanceId));
		networkPolicySpec.setPodSelector(podSelector);

		networkPolicy.setSpec(networkPolicySpec);

		V1NetworkPolicy networkPolicy1 = networkingV1Api.createNamespacedNetworkPolicy(
			"default",
			networkPolicy,
			null,
			null,
			null);
		System.out.println("Created network policy");
		System.out.println(networkPolicy1);
	}

	void deleteNetworkPolicy() throws ApiException {
		System.out.println("Deleting network policy");
		V1Status status = networkingV1Api.deleteNamespacedNetworkPolicy(
			"network-policy-quack",
			"default",
			null,
			null,
			null,
			null,
			null,
			null
		);
		System.out.println("Deleted network policy");
		System.out.println(status);
	}

}

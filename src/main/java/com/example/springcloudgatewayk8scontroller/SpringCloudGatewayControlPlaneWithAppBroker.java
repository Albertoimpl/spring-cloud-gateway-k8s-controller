package com.example.springcloudgatewayk8scontroller;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicyIngressRule;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicyPeer;
import io.fabric8.kubernetes.api.model.networking.NetworkPolicySpec;
import io.fabric8.kubernetes.client.KubernetesClient;
import reactor.core.publisher.Mono;

import org.springframework.cloud.appbroker.service.CreateServiceInstanceAppBindingWorkflow;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

@Service
public class SpringCloudGatewayControlPlaneWithAppBroker {

	private ServiceInstanceService workflowServiceInstanceService;

	private ServiceInstanceBindingService workflowServiceInstanceBindingService;

	public SpringCloudGatewayControlPlaneWithAppBroker(ServiceInstanceService workflowServiceInstanceService,
		ServiceInstanceBindingService workflowServiceInstanceBindingService) {
		this.workflowServiceInstanceService = workflowServiceInstanceService;
		this.workflowServiceInstanceBindingService = workflowServiceInstanceBindingService;
	}

	/**
	 * When
	 * a kind:SpringCloudGateway gets created
	 * or
	 * a svcat create-service spring-cloud-gateway my-gateway
	 */
	public Mono<Void> onCreate() {
		CreateServiceInstanceRequest request = createRequestFromExistingCatalog();
		return workflowServiceInstanceService
			.createServiceInstance(request)
			.then();
	}

	/**
	 * In order for it to become a broker if needed, we would need a catalog
	 * This is not particularly relevant but it will enable some use cases
	 */
	private CreateServiceInstanceRequest createRequestFromExistingCatalog() {
		return CreateServiceInstanceRequest
			.builder()
			.serviceInstanceId("random")
			.planId("92528b61-e748-4a59-9091-23e16570443f")
			.plan(Plan
				.builder()
				.id("92528b61-e748-4a59-9091-23e16570443f")
				.name("gateway-plan")
				.build())
			.serviceDefinitionId("ec457964-6cd7-47a1-859f-f652ca7d315f")
			.serviceDefinition(
				ServiceDefinition
					.builder()
					.id("ec457964-6cd7-47a1-859f-f652ca7d315f")
					.name("gateway-service")
					.build())
			.build();
	}

	/**
	 * When
	 * an application with a "binds-to=my-gateway" label gets created
	 * or
	 * a svcat bind-service my-gateway --label=binds-to=my-app
	 */
	public Mono<Void> onBind() {
		CreateServiceInstanceBindingRequest request =
			CreateServiceInstanceBindingRequest.builder().build();

		return workflowServiceInstanceBindingService
			.createServiceInstanceBinding(request)
			.then();
	}

}

/**
 * Imperative declaration of a workflow that gets executed on binding
 * This is all the creator of the service will need to worry about
 */
@Service
class NetworkPolicyConfigurer implements CreateServiceInstanceAppBindingWorkflow {

	KubernetesClient kubernetesClient;

	public NetworkPolicyConfigurer(KubernetesClient kubernetesClient) {
		this.kubernetesClient = kubernetesClient;
	}

	@Override
	public Mono<Void> create(CreateServiceInstanceBindingRequest request,
		CreateServiceInstanceAppBindingResponse response) {

		String myAppLabel = (String) request.getParameters().get("label");
		String serviceInstanceId = request.getServiceInstanceId();

		return createNetworkPolicy(myAppLabel, serviceInstanceId);
	}

	public Mono<Void> createNetworkPolicy(String clientAppLabel, String serviceInstanceId) {
		NetworkPolicy networkPolicy = new NetworkPolicy();
		networkPolicy.setApiVersion("networking.k8s.io/v1");
		networkPolicy.setKind("NetworkPolicy");
		ObjectMeta objectMeta = new ObjectMeta();
		objectMeta.setName("network-policy-" + serviceInstanceId);
		objectMeta.setNamespace("default"); // I know, I know...
		networkPolicy.setMetadata(objectMeta);

		NetworkPolicySpec networkPolicySpec = new NetworkPolicySpec();

		LabelSelector labelSelector = new LabelSelector();
		labelSelector.setMatchLabels(singletonMap("service-instance-id", serviceInstanceId));
		networkPolicySpec.setPodSelector(labelSelector);

		networkPolicySpec.setPolicyTypes(singletonList("Ingress"));
		NetworkPolicyIngressRule ingressRule = new NetworkPolicyIngressRule();
		NetworkPolicyPeer networkPolicyPeer = new NetworkPolicyPeer();
		NetworkPolicySpec ingressSpec = new NetworkPolicySpec();
		LabelSelector clientAppSelector = new LabelSelector();
		clientAppSelector.setMatchLabels(singletonMap("binds-to", clientAppLabel));
		ingressSpec.setPodSelector(clientAppSelector);
		networkPolicy.setSpec(ingressSpec);
		ingressRule.setFrom(singletonList(networkPolicyPeer));

		networkPolicySpec.setIngress(singletonList(ingressRule));

		networkPolicy.setSpec(networkPolicySpec);
		kubernetesClient.network().networkPolicies().create(networkPolicy);

		return Mono.empty();
	}

}

package com.example.springcloudgatewayk8scontroller;

import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import com.vmware.tanzu.springcloudgateway.SpringCloudGatewayV1Api;
import com.vmware.tanzu.springcloudgateway.V1SpringCloudGateway;
import com.vmware.tanzu.springcloudgateway.V1SpringCloudGatewayList;
import io.kubernetes.client.extended.controller.Controller;
import io.kubernetes.client.extended.controller.DefaultControllerWatch;
import io.kubernetes.client.extended.controller.builder.ControllerBuilder;
import io.kubernetes.client.extended.controller.builder.ControllerManagerBuilder;
import io.kubernetes.client.extended.controller.reconciler.Reconciler;
import io.kubernetes.client.extended.controller.reconciler.Request;
import io.kubernetes.client.extended.controller.reconciler.Result;
import io.kubernetes.client.extended.workqueue.WorkQueue;
import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiCallback;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.AppsV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.models.V1DeploymentList;
import io.kubernetes.client.openapi.models.V1NetworkPolicyList;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;

public class SpringCloudGatewayControlPlaneWithCRDReconciler {

	private static final String CONTROLLER_NAME = "SpringCloudGatewayController";

	private static final int WORKER_COUNT = 4;

	static class SpringCloudGatewayReconciler implements Reconciler {

		private Lister<V1SpringCloudGateway> lister;

		private NetworkPolicyService networkPolicyService;

		private DeploymentService deploymentService;

		public SpringCloudGatewayReconciler(
			SharedIndexInformer<V1SpringCloudGateway> informer,
			NetworkPolicyService networkPolicyService,
			DeploymentService deploymentService) {
			this.lister = new Lister<>(informer.getIndexer());
			this.networkPolicyService = networkPolicyService;
			this.deploymentService = deploymentService;
		}

		@Override
		public Result reconcile(Request request) {
			try {
				String namespace = request.getNamespace();
				if (!"default".equals(namespace)) {
					return new Result(false);
				}

				String name = request.getName();
				V1SpringCloudGateway gateway = lister.namespace(namespace).get(name);
				if (gateway == null) {
					System.out.println("******* Network policies");
					V1NetworkPolicyList networkingV1ApiList = networkPolicyService.listNetworkPolicies();
					System.out.println(networkingV1ApiList.getItems().size());
					if (!networkingV1ApiList.getItems().isEmpty()) {
						networkPolicyService.deleteNetworkPolicy();
					}

					System.out.println("******* Deployments");
					V1DeploymentList deploymentList = deploymentService.listDeployments();
					System.out.println(deploymentList.getItems().size());
					if (!deploymentList.getItems().isEmpty()) {
						deploymentService.deleteDeployment();
					}

					return new Result(false);
				}
				System.out.println("******* Gateway");
				System.out.println(gateway.getMetadata().getLabels());

				System.out.println("******* Network policies");
				V1NetworkPolicyList networkingV1ApiList = networkPolicyService.listNetworkPolicies();
				System.out.println(networkingV1ApiList.getItems().size());
				if (networkingV1ApiList.getItems().isEmpty()) {
					networkPolicyService.createNetworkPolicy();
				}

				System.out.println("******* Deployments");
				V1DeploymentList deploymentList = deploymentService.listDeployments();
				System.out.println(deploymentList.getItems().size());
				if (deploymentList.getItems().isEmpty()) {
					deploymentService.createDeployment();
				}

			}
			catch (ApiException e) {
				e.printStackTrace();
				return new Result(false);
			}
			return new Result(false);
		}

	}

	public static void main(String[] args) throws IOException {
		ApiClient client = initializeApiClient();
		SpringCloudGatewayV1Api springCloudGatewayApi = new SpringCloudGatewayV1Api(client);

		NetworkingV1Api networkApiClient = new NetworkingV1Api(client);
		NetworkPolicyService networkPolicyService = new NetworkPolicyService(networkApiClient);

		AppsV1Api appsV1Api = new AppsV1Api(client);
		DeploymentService deploymentService = new DeploymentService(appsV1Api);

		SharedInformerFactory factory = new SharedInformerFactory();
		ControllerManagerBuilder managerBuilder = ControllerBuilder.controllerManagerBuilder(factory);

		SharedIndexInformer<V1SpringCloudGateway> informer = factory.sharedIndexInformerFor(
			callGeneratorParams -> springCloudGatewayApi.listSpringCloudGatewayForAllNamespacesCall(
				null,
				null,
				null,
				null,
				null,
				null,
				callGeneratorParams.resourceVersion,
				callGeneratorParams.timeoutSeconds,
				callGeneratorParams.watch,
				SpringCloudGatewayListCallback()),
			V1SpringCloudGateway.class,
			V1SpringCloudGatewayList.class);

		Controller controller = ControllerBuilder.defaultBuilder(factory).watch(
			(workQueue) -> createControllerWatch(workQueue))
			.withReconciler(new SpringCloudGatewayReconciler(informer, networkPolicyService, deploymentService))
			.withName(CONTROLLER_NAME)
			.withWorkerCount(WORKER_COUNT)
			.build();

		managerBuilder.addController(controller).build().run();
	}

	private static ApiClient initializeApiClient() throws IOException {
		String home = System.getProperty("user.home");
		String kubeConfigPath = home + "/.kube/config";

		// In a cluster we should be able to read the KUBECONFIG variable as path
		String kubeconfig = System.getProperty("KUBECONFIG");
		if (kubeconfig != null && !kubeconfig.isEmpty()) {
			kubeConfigPath = kubeconfig;
		}

		ApiClient client =
			ClientBuilder.kubeconfig(KubeConfig.loadKubeConfig(new FileReader(kubeConfigPath)))
				.build();
		Configuration.setDefaultApiClient(client);

		return client;
	}

	private static DefaultControllerWatch<V1SpringCloudGateway> createControllerWatch(WorkQueue<Request> workQueue) {
		return ControllerBuilder.controllerWatchBuilder(V1SpringCloudGateway.class, workQueue)
			.withWorkQueueKeyFunc(
				pod -> new Request("default", "my-gateway-with-crd"))
			.withOnAddFilter(pod -> {
				String namespace = "default";
				String name = "my-gateway-with-crd";

				System.out.println(String.format(
					"[%s] Event: Add SpringCloudGateway '%s/%s'", CONTROLLER_NAME, namespace, name));
				return true;
			})
			.withOnUpdateFilter((oldSpringCloudGateway, newSpringCloudGateway) -> {
				String namespace = "default";
				String name = "my-gateway-with-crd";

				System.out.println(String.format(
					"[%s] Event: Update SpringCloudGateway '%s/%s'", CONTROLLER_NAME, namespace, name));

				return true;
			})
			.withOnDeleteFilter((pod, aBoolean) -> {
				String namespace = "default";
				String name = "my-gateway-with-crd";

				System.out.println(String.format(
					"[%s] Event: Delete SpringCloudGateway '%s/%s'", CONTROLLER_NAME, namespace, name));
				return true;
			})
			.build();
	}

	private static ApiCallback<V1SpringCloudGatewayList> SpringCloudGatewayListCallback() {
		return new ApiCallback<V1SpringCloudGatewayList>() {
			@Override
			public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {

			}

			@Override
			public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {

			}

			@Override
			public void onSuccess(V1SpringCloudGatewayList result, int statusCode, Map responseHeaders) {
				System.out.println("onSuccess SharedIndexInformer");
				System.out.println(statusCode);
			}

			@Override
			public void onFailure(ApiException e, int statusCode, Map responseHeaders) {
				System.out.println("onFailure SharedIndexInformer");
				System.out.println(statusCode);
				e.printStackTrace();
			}
		};
	}

}

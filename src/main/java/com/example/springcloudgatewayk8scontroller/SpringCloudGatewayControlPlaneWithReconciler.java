package com.example.springcloudgatewayk8scontroller;

import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

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
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.apis.NetworkingV1Api;
import io.kubernetes.client.openapi.models.V1NetworkPolicyList;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1PodList;
import io.kubernetes.client.util.ClientBuilder;
import io.kubernetes.client.util.KubeConfig;

public class SpringCloudGatewayControlPlaneWithReconciler {

	private static final String CONTROLLER_NAME = "SpringCloudGatewayController";

	private static final int WORKER_COUNT = 4;

	static class SpringCloudGatewayReconciler implements Reconciler {

		private Lister<V1Pod> podLister;

		private NetworkPolicyService networkPolicyService;

		public SpringCloudGatewayReconciler(
			SharedIndexInformer<V1Pod> podInformer,
			NetworkPolicyService networkPolicyService) {
			this.podLister = new Lister<>(podInformer.getIndexer());
			this.networkPolicyService = networkPolicyService;
		}

		@Override
		public Result reconcile(Request request) {
			try {
				String namespace = request.getNamespace();
				if (!"default".equals(namespace)) {
					return new Result(false);
				}

				String name = request.getName();
				V1Pod pod = podLister.namespace(namespace).get(name);
				if (pod == null) {
					V1NetworkPolicyList networkingV1ApiList =
						networkPolicyService.listNetworkPolicies();
					System.out.println("******* Network policies");
					System.out.println(networkingV1ApiList.getItems().size());

					if (!networkingV1ApiList.getItems().isEmpty()) {
						networkPolicyService.deleteNetworkPolicy();
					}
					return new Result(false);
				}
				System.out.println("******* POD");
				System.out.println(pod.getMetadata().getLabels());

				if (!pod.getMetadata().getLabels().containsKey("service-instance-id")) {
					return new Result(false);
				}

				V1NetworkPolicyList networkingV1ApiList = networkPolicyService.listNetworkPolicies();
				System.out.println("******* Network policies");
				System.out.println(networkingV1ApiList.getItems().size());
				if (networkingV1ApiList.getItems().isEmpty()) {
					networkPolicyService.createNetworkPolicy();
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
		CoreV1Api coreV1Api = new CoreV1Api(client);
		NetworkingV1Api networkApiClient = new NetworkingV1Api(client);
		NetworkPolicyService networkPolicyService = new NetworkPolicyService(networkApiClient);

		SharedInformerFactory factory = new SharedInformerFactory();
		ControllerManagerBuilder managerBuilder = ControllerBuilder.controllerManagerBuilder(factory);

		SharedIndexInformer<V1Pod> podInformer = factory.sharedIndexInformerFor(
			callGeneratorParams -> coreV1Api.listPodForAllNamespacesCall(
				null,
				null,
				null,
				null,
				null,
				null,
				callGeneratorParams.resourceVersion,
				callGeneratorParams.timeoutSeconds,
				callGeneratorParams.watch,
				PodListCallback()),
			V1Pod.class,
			V1PodList.class);

		Controller controller = ControllerBuilder.defaultBuilder(factory).watch(
			(workQueue) -> createControllerWatch(workQueue))
			.withReconciler(new SpringCloudGatewayReconciler(podInformer, networkPolicyService))
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

	private static DefaultControllerWatch<V1Pod> createControllerWatch(WorkQueue<Request> workQueue) {
		return ControllerBuilder.controllerWatchBuilder(V1Pod.class, workQueue)
			.withWorkQueueKeyFunc(
				pod -> new Request(Objects.requireNonNull(pod.getMetadata()).getNamespace(), pod.getMetadata().getName()))
			.withOnAddFilter(pod -> {
				String namespace = Objects.requireNonNull(pod.getMetadata()).getNamespace();
				String name = pod.getMetadata().getName();

				System.out.println(String.format(
					"[%s] Event: Add Pod '%s/%s'", CONTROLLER_NAME, namespace, name));
				return true;
			})
			.withOnUpdateFilter((oldPod, newPod) -> {
				String namespace = Objects.requireNonNull(oldPod.getMetadata()).getNamespace();
				String name = oldPod.getMetadata().getName();

				System.out.println(String.format(
					"[%s] Event: Update Pod '%s/%s'", CONTROLLER_NAME, namespace, name));

				return true;
			})
			.withOnDeleteFilter((pod, aBoolean) -> {
				String namespace = Objects.requireNonNull(pod.getMetadata()).getNamespace();
				String name = pod.getMetadata().getName();

				System.out.println(String.format(
					"[%s] Event: Delete Pod '%s/%s'", CONTROLLER_NAME, namespace, name));
				return true;
			})
			.build();
	}

	private static ApiCallback<V1PodList> PodListCallback() {
		return new ApiCallback<V1PodList>() {
			@Override
			public void onUploadProgress(long bytesWritten, long contentLength, boolean done) {

			}

			@Override
			public void onDownloadProgress(long bytesRead, long contentLength, boolean done) {

			}

			@Override
			public void onSuccess(V1PodList result, int statusCode, Map responseHeaders) {
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

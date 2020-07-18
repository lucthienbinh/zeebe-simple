package com.zeebe.simple.zeebesimple;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import io.zeebe.client.api.response.ActivatedJob;
import io.zeebe.client.api.response.WorkflowInstanceEvent;
import io.zeebe.client.api.worker.JobClient;
import io.zeebe.spring.client.EnableZeebeClient;
import io.zeebe.spring.client.ZeebeClientLifecycle;
import io.zeebe.spring.client.annotation.ZeebeDeployment;
import io.zeebe.spring.client.annotation.ZeebeWorker;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableZeebeClient
@ZeebeDeployment(classPathResources = "demoProcess.bpmn")
@Slf4j
public class SimpleApplication {

	public static void main(String[] args) {
		SpringApplication.run(SimpleApplication.class, args);
	}
	
	private static void logJob(final ActivatedJob job) {
		log.info(
		  "complete job\n>>> [type: {}, key: {}, element: {}, workflow instance: {}]\n{deadline; {}]\n[headers: {}]\n[variables: {}]",
		  job.getType(),
		  job.getKey(),
		  job.getElementId(),
		  job.getWorkflowInstanceKey(),
		  Instant.ofEpochMilli(job.getDeadline()),
		  job.getCustomHeaders(),
		  job.getVariables());
	}

	@ZeebeWorker(type = "type_A")
	public void handleTypeAJob(final JobClient client, final ActivatedJob job) throws InterruptedException {

		Thread.sleep(10000);
		final Map<String, Object> variables = job.getVariablesAsMap();
		variables.put("type_A", "done");
		logJob(job);
		client.newCompleteCommand(job.getKey()).variables(variables).send().join();
	}

	@ZeebeWorker(type = "type_B")
	public void handleTypeBJob(final JobClient client, final ActivatedJob job) throws InterruptedException {

		Thread.sleep(10000);
		final Map<String, Object> variables = job.getVariablesAsMap();
		variables.put("type_B", "done");
		logJob(job);
		client.newCompleteCommand(job.getKey()).variables(variables).send().join();
	}

	@ZeebeWorker(type = "type_C")
	public void handleTypeCJob(final JobClient client, final ActivatedJob job) throws InterruptedException {

		Thread.sleep(10000);
		final Map<String, Object> variables = job.getVariablesAsMap();
		variables.put("type_C", "done");
		logJob(job);
		client.newCompleteCommand(job.getKey()).variables(variables).send().join();
	}

}

@EnableZeebeClient
@Slf4j
@Controller
@RequestMapping(path = "/zeebe")
class ZeebeClientClass {

    @Autowired
	private ZeebeClientLifecycle client;

    @GetMapping("/startProcess")
	ResponseEntity<?> startProcess() {

		if (!client.isRunning()) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		final WorkflowInstanceEvent event =
		client
			.newCreateInstanceCommand()
			.bpmnProcessId("demoProcess")
			.latestVersion()
			.variables("{\"a\": \"" + UUID.randomUUID().toString() + "\"}")
			.send()
			.join();

		log.info("started instance for workflowKey='{}', bpmnProcessId='{}', version='{}' with workflowInstanceKey='{}'",
		event.getWorkflowKey(), event.getBpmnProcessId(), event.getVersion(), event.getWorkflowInstanceKey());

		String response = "started instance for workflowKey=" + event.getWorkflowKey() + ", workflowInstanceKey=" + event.getWorkflowInstanceKey();
		
		return new ResponseEntity<>(response, HttpStatus.OK);
	}

	@GetMapping("/stopProcess/{instanceKey}")
	ResponseEntity<?> stopProcess(@PathVariable("instanceKey") long instanceKey) {

		if (!client.isRunning()) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		client.newCancelInstanceCommand(instanceKey).send();

		log.info("stoped instance with workflowInstanceKey='{}'",
		instanceKey);

		String response = "stoped workflowInstanceKey=" + instanceKey;

		return new ResponseEntity<>(response, HttpStatus.OK);
	}


}



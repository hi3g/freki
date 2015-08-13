#!/usr/bin/env python
from locust import HttpLocust, TaskSet, task
import json
import datetime

def get_json_header():
	return {"content-type":"application/json"}

def get_epoch_timestamp():
	return (time.clock()* 1000)

def get_l_datapoints_data():
	return [{"metric":"locust.long", "timestamp": get_epoch_timestamp(), "longValue": 3252752, "tags": {"host": "localhost"}}]

def get_d_datapoints_data():
	return [{"metric":"locust.double", "timestamp": get_epoch_timestamp(), "doubleValue": 3252752.0, "tags": {"host": "localhost"}}]

def get_f_datapoints_data():
	return [{"metric":"locust.float", "timestamp": get_epoch_timestamp(), "floatValue": 3252752.0, "tags": {"host": "localhost"}}]

class UserBehavior(TaskSet):

	@task(1)
	def add_long_datapoint(self):
		data = get_l_datapoints_data();
		self.client.post("/datapoints", data=json.dumps(data), headers=get_json_header())

	@task(1)
	def add_double_datapoint(self):
		data = get_d_datapoints_data();
		self.client.post("/datapoints", data=json.dumps(data), headers=get_json_header())

	@task(1)
	def add_float_datapoint(self):
		data = get_f_datapoints_data();
		self.client.post("/datapoints", data=json.dumps(data), headers=get_json_header())

class WebsiteUser(HttpLocust):
	task_set = UserBehavior
	min_wait = 10
	max_wait = 50
	host = "http://localhost:8080"
	
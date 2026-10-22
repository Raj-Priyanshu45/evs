package com.wastewise.api.model;

/** One label returned by the Hugging Face image classifier. */
public record ImagePrediction(String label, double score) { }

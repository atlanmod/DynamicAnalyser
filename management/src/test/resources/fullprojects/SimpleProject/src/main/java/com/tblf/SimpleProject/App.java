package com.tblf.SimpleProject;
public class App implements IApp
{
	public App() {
		System.out.println("Constructor called");
	}	

	public App(String string) {
		System.out.println(string);
	}	

	public void method() {
		System.out.println("The method");
		System.out.println("has been called");
	}
}

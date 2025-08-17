package net.signfinder.services;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service registry for dependency injection.
 * Provides centralized service management and reduces coupling between
 * components.
 */
public class ServiceRegistry
{
	private static final Logger LOGGER =
		LoggerFactory.getLogger(ServiceRegistry.class);
	
	private static final Map<Class<?>, Object> services =
		new ConcurrentHashMap<>();
	private static final Map<Class<?>, Class<?>> bindings =
		new ConcurrentHashMap<>();
	
	/**
	 * Register a service instance.
	 */
	public static <T> void registerService(Class<T> serviceClass,
		T implementation)
	{
		services.put(serviceClass, implementation);
		LOGGER.debug("Registered service: {} -> {}",
			serviceClass.getSimpleName(),
			implementation.getClass().getSimpleName());
	}
	
	/**
	 * Bind an interface to an implementation class.
	 */
	public static <T> void bind(Class<T> interfaceClass,
		Class<? extends T> implementationClass)
	{
		bindings.put(interfaceClass, implementationClass);
		LOGGER.debug("Bound interface: {} -> {}",
			interfaceClass.getSimpleName(),
			implementationClass.getSimpleName());
	}
	
	/**
	 * Get a service instance.
	 *
	 * @param serviceClass
	 *            The service class or interface
	 * @return The service instance
	 * @throws ServiceNotFoundException
	 *             if service is not found
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getService(Class<T> serviceClass)
	{
		// First check for direct service registration
		Object service = services.get(serviceClass);
		if(service != null)
		{
			return (T)service;
		}
		
		// Check for interface binding
		Class<?> implementationClass = bindings.get(serviceClass);
		if(implementationClass != null)
		{
			service = services.get(implementationClass);
			if(service != null)
			{
				return (T)service;
			}
		}
		
		throw new ServiceNotFoundException(
			"Service not found: " + serviceClass.getName());
	}
	
	/**
	 * Check if a service is registered.
	 */
	public static boolean hasService(Class<?> serviceClass)
	{
		return services.containsKey(serviceClass)
			|| (bindings.containsKey(serviceClass)
				&& services.containsKey(bindings.get(serviceClass)));
	}
	
	/**
	 * Unregister a service.
	 */
	public static void unregisterService(Class<?> serviceClass)
	{
		Object removed = services.remove(serviceClass);
		if(removed != null)
		{
			LOGGER.debug("Unregistered service: {}",
				serviceClass.getSimpleName());
		}
	}
	
	/**
	 * Clear all registered services.
	 * Primarily for testing purposes.
	 */
	public static void clear()
	{
		services.clear();
		bindings.clear();
		LOGGER.debug("Service registry cleared");
	}
	
	/**
	 * Get number of registered services.
	 */
	public static int getServiceCount()
	{
		return services.size();
	}
	
	/**
	 * Exception thrown when a requested service is not found.
	 */
	public static class ServiceNotFoundException extends RuntimeException
	{
		public ServiceNotFoundException(String message)
		{
			super(message);
		}
	}
}

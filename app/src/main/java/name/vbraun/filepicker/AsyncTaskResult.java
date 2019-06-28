package name.vbraun.filepicker;

public interface AsyncTaskResult<T extends Object> {
	public void taskFinish(T result);
}

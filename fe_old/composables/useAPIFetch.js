export const useAPIFetch = (request, opts = {}) => {
  const config = useRuntimeConfig();
  opts.baseURL = config.public.baseURL;
  return useFetch(request, opts)
}
import {useCallback, useRef} from 'react';
import {useQuery, useMutation, useQueryClient} from '@tanstack/react-query';
import {db} from '../constants/db';
import {
  getInkCanvasState,
  saveInkCanvasState,
} from '../services/inkCanvas';
import type {InkCanvasRef} from '../components/InkCanvas';

const QUERY_KEY_PREFIX = 'inkCanvasState';

/**
 * Hook for managing ink canvas state persistence.
 *
 * @param canvasId - Unique identifier for the canvas
 * @returns Object containing the canvas ref, initial strokes, loading state, and save handler
 *
 * @example
 * ```tsx
 * function DrawingScreen() {
 *   const {canvasRef, initialStrokes, isLoading, handleStrokesChange} =
 *     useInkCanvasPersistence('main-canvas');
 *
 *   if (isLoading) {
 *     return <ActivityIndicator />;
 *   }
 *
 *   return (
 *     <InkCanvas
 *       ref={canvasRef}
 *       initialStrokes={initialStrokes}
 *       onStrokesChange={handleStrokesChange}
 *     />
 *   );
 * }
 * ```
 */
export function useInkCanvasPersistence(canvasId: string) {
  const canvasRef = useRef<InkCanvasRef>(null);
  const queryClient = useQueryClient();

  // Query to load initial strokes
  const {data, isLoading, error} = useQuery({
    queryKey: [QUERY_KEY_PREFIX, canvasId],
    queryFn: async () => {
      const state = await getInkCanvasState(db, canvasId);
      return state?.strokesJson ?? '[]';
    },
    staleTime: Infinity, // Don't refetch unless explicitly invalidated
  });

  // Mutation to save strokes
  const saveMutation = useMutation({
    mutationFn: async (strokesJson: string) => {
      await saveInkCanvasState(db, canvasId, strokesJson);
    },
    onSuccess: (_data, strokesJson) => {
      // Update the cache directly instead of refetching
      queryClient.setQueryData([QUERY_KEY_PREFIX, canvasId], strokesJson);
    },
  });

  // Debounced save handler to avoid saving on every stroke
  const saveTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const handleStrokesChange = useCallback(
    (strokesJson: string) => {
      // Clear any pending save
      if (saveTimeoutRef.current) {
        clearTimeout(saveTimeoutRef.current);
      }

      // Debounce the save by 500ms
      saveTimeoutRef.current = setTimeout(() => {
        saveMutation.mutate(strokesJson);
      }, 500);
    },
    [saveMutation],
  );

  return {
    canvasRef,
    initialStrokes: data,
    isLoading,
    error,
    handleStrokesChange,
    isSaving: saveMutation.isPending,
  };
}
